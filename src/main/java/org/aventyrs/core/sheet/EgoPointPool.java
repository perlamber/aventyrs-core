package org.aventyrs.core.sheet;

import org.aventyrs.core.character.EgoDomain;

import java.util.HashMap;
import java.util.Map;

/**
 * One {@link EgoDomain}'s two spendable Ego point pools on a {@link CombatantSheet} — the
 * permanent one (whose maximum is the Ego stat itself) and the temporary one stacked on top of
 * it — plus the per-source bonuses that widen the temporary half.
 *
 * <h2>The model</h2>
 * <pre>
 * permanentMax        = character.getEgos().getEgo(domain).getTotal()   // base + variable
 * permanentRemaining  = max(0, permanentMax - permanentSpent)
 * temporaryCeiling    = max(0, permanentRemaining + Σ bonus(source) - Σ activeEgoPenalty)
 * temporaryRemaining  = max(0, temporaryCeiling - temporarySpent)
 * availableEgoPoints  = permanentRemaining + temporaryRemaining
 * </pre>
 *
 * <p>The temporary ceiling tracks permanent points <em>remaining</em>, not the permanent
 * maximum, so <strong>spending a permanent point hurts twice</strong>: it removes the point and
 * lowers the ceiling above it. That falls straight out of the arithmetic — nothing special-cases
 * it. Worked example, Sorte 3 with nothing spent: 3 permanent + 3 temporary = 6 spendable.
 * Spending the 3 temporary first, then the 3 permanent, yields all 6. Spending the 3 permanent
 * <em>first</em> collapses the ceiling to 0, yielding only 3.
 *
 * <p>Both pools start <strong>full</strong>: a freshly built sheet has spent nothing.
 *
 * <h2>Why a spent-counter and not a held balance</h2>
 * Both halves are {@link ResourcePool}s — "how much has been spent against an externally computed
 * maximum" — and this class owns only the ceiling derivation on top of them. The alternative, a
 * directly-held balance — which is how temporary Ego points were modelled before this — cannot
 * work here: every
 * change to the ceiling (a permanent spend, a {@link TemporaryEgoPenalty} landing, one expiring,
 * a permanent point earned) would need a destructive clamp pushed at the balance from four
 * separate call sites, and such a clamp is irreversible — an expiring penalty could never give
 * back the point it truncated. With a spent counter the ceiling is only ever <em>read</em>, so
 * what remains is {@code max(0, ceiling - spent)} computed fresh, and it un-clamps by itself when
 * the ceiling returns. Same recompute-on-demand discipline as {@code HitPointsService#getStatus}
 * and {@code InitiativeEntry#getEffectiveInitiativeValue}.
 *
 * <p>The two ceiling inputs this class does <em>not</em> own — {@code permanentMax} and the
 * active penalty total — are passed in per call, keeping {@link ResourcePool}'s own "externally
 * computed maximum" contract. {@link AbstractCombatantSheet} is what resolves both.
 *
 * <h2>The one rule the formula doesn't give you</h2>
 * {@code temporarySpent} is <strong>normalized down to the current ceiling</strong>, lazily, at
 * the top of every temporary-facing operation. Without it, "spent 3 temporary, then spent 1
 * permanent (ceiling → 2)" would leave spent=3 against a ceiling of 2, and the next recovery
 * would be silently swallowed restoring nothing — a double punishment the rules never state.
 * Note the deliberate contrast with {@link ResourcePool} as used for Hit Points, where
 * overspending past the maximum is <em>kept</em> on purpose, because that negative range is what
 * distinguishes FALLEN/COMMA/DEAD. Ego points have no negative range to mean anything.
 *
 * <p>The normalization is lossy in exactly one direction, an accepted simplification flagged the
 * same way {@link PendingEgoRecovery}'s and {@code CriticalResult}'s own inferences are: spend
 * truncated while a penalty was active is not restored if a permanent point is later
 * <em>earned</em> while still over-spent. Tracking pre-clamp overspend to fix that would buy
 * correctness only in a case requiring a permanent Ego point to be earned mid-session, in a
 * domain that is simultaneously over-spent and penalized.
 */
class EgoPointPool {
    private final ResourcePool permanent = new ResourcePool();
    private final ResourcePool temporary = new ResourcePool();

    /**
     * Each source's own current contribution to the temporary ceiling — see {@link
     * #grantTemporaryBonus}. Absent is the same as 0; a source is only added the first time it
     * grants.
     */
    private final Map<Object, Integer> temporaryBonusContributions = new HashMap<>();

    /** Permanent points not yet spent. Floors at 0. */
    int getPermanentRemaining(final int permanentMax) {
        return Math.max(0, permanentMax - permanent.getSpent());
    }

    /**
     * How many temporary points this domain may hold right now: permanent points remaining, plus
     * every source's bonus, minus {@code penalty}. Floors at 0.
     */
    int getTemporaryCeiling(final int permanentMax, final int penalty) {
        return Math.max(0, getPermanentRemaining(permanentMax) + sumTemporaryBonuses() - penalty);
    }

    /** Temporary points not yet spent, under the live ceiling. */
    int getTemporaryRemaining(final int permanentMax, final int penalty) {
        int ceiling = getTemporaryCeiling(permanentMax, penalty);
        clampTemporarySpent(ceiling);
        return ceiling - temporary.getSpent();
    }

    /**
     * Spends up to {@code amount} permanent points, returning how many were <em>actually</em>
     * spent — never more than remain. Also lowers the temporary ceiling as a consequence; see
     * this class's own javadoc.
     */
    int spendPermanent(final int permanentMax, final int amount) {
        int spendable = Math.min(Math.max(0, amount), getPermanentRemaining(permanentMax));
        permanent.spend(spendable);
        return spendable;
    }

    /**
     * Spends up to {@code amount} temporary points, returning how many were <em>actually</em>
     * spent — never more than remain under the live ceiling.
     */
    int spendTemporary(final int permanentMax, final int penalty, final int amount) {
        int spendable = Math.min(Math.max(0, amount), getTemporaryRemaining(permanentMax, penalty));
        temporary.spend(spendable);
        return spendable;
    }

    /**
     * Restores up to {@code amount} previously-spent temporary points, returning how many
     * <em>actually</em> came back — bounded by what was spent, so a recovery can never push a
     * pool above its own ceiling.
     */
    int recoverTemporary(final int permanentMax, final int penalty, final int amount) {
        clampTemporarySpent(getTemporaryCeiling(permanentMax, penalty));
        int recoverable = Math.min(Math.max(0, amount), temporary.getSpent());
        temporary.recover(recoverable);
        return recoverable;
    }

    /**
     * Raises {@code source}'s own contribution to the temporary ceiling to at least
     * {@code amount}, without that one source stacking a second point on top of one it already
     * granted — e.g. {@code org.aventyrs.core.ability.CharismaAbility#DESTINO_FAVORAVEL}'s "um
     * ponto temporário, não cumulativo": repeated triggers of that same ability don't widen the
     * ceiling twice. {@code source} identifies <em>which</em> ability/effect is granting (the
     * enum constant itself, typically) — only that one source's own repeat grants are capped; an
     * unrelated source's grant still adds on top, since "não cumulativo" is only ever about one
     * source's own repeated triggers.
     *
     * <p>There is deliberately no cumulative counterpart: nothing grants a temporary Ego point
     * cumulatively today, and the {@code source} parameter is what signals this one's shape.
     */
    int grantTemporaryBonus(final Object source, final int amount) {
        int previous = temporaryBonusContributions.getOrDefault(source, 0);
        temporaryBonusContributions.put(source, Math.max(previous, amount));
        return sumTemporaryBonuses();
    }

    private int sumTemporaryBonuses() {
        return temporaryBonusContributions.values().stream().mapToInt(Integer::intValue).sum();
    }

    /** See "The one rule the formula doesn't give you" in this class's own javadoc. */
    private void clampTemporarySpent(final int ceiling) {
        int excess = temporary.getSpent() - ceiling;
        if (excess > 0) {
            temporary.recover(excess);
        }
    }
}
