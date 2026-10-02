package org.aventyrs.core.sheet;

import org.aventyrs.core.character.EgoDomain;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One {@link EgoDomain}'s two spendable Ego point pools on a {@link CombatantSheet} — the
 * permanent one (whose maximum is the Ego stat itself) and the temporary one stacked on top of
 * it — plus the <em>extra</em> temporary points held above that temporary pool.
 *
 * <h2>The model</h2>
 * <pre>
 * permanentMax        = character.getEffectiveEgoTotal(domain)   // base + variable + Talentos, ≤ 5
 * permanentRemaining  = max(0, permanentMax - permanentSpent)
 * temporaryCeiling    = max(0, permanentRemaining - Σ activeEgoPenalty)
 * temporaryRemaining  = max(0, temporaryCeiling - temporarySpent) + extras
 * availableEgoPoints  = permanentRemaining + temporaryRemaining
 * </pre>
 *
 * <p>The temporary ceiling tracks permanent points <em>remaining</em>, not the permanent
 * maximum, so <strong>spending a permanent point hurts twice</strong>: it removes the point and
 * lowers the ceiling above it. Worked example, Sorte 3 with nothing spent: 3 permanent + 3
 * temporary = 6 spendable. Spending the 3 temporary first, then the 3 permanent, yields all 6.
 * Spending the 3 permanent <em>first</em> collapses the ceiling to 0, yielding only 3.
 *
 * <h2>A permanent spend and the points still held (table ruling, 2026-09-30)</h2>
 * Lowering the ceiling never takes a temporary point the character still holds, unless it no longer
 * fits: held = min(held, new ceiling). The book's Recursos example — "Recursos 5 (3 disponíveis)"
 * spends 2 temporary, then 1 permanent, and "fica com Recursos total 4 (1 disponível)" — would reach
 * 0 by subtracting the spent count from the lowered ceiling. So a permanent spend of {@code k} also
 * takes {@code k} back off {@code temporarySpent} (floored at 0), which keeps what is held unchanged,
 * and the normalization below clips it if it no longer fits. Both extremes of the Sorte 3 example are
 * unchanged.
 *
 * <p>Both pools start <strong>full</strong>: a freshly built sheet has spent nothing.
 *
 * <h2>Received points and extras (table ruling, 0.0.76)</h2>
 * Two ways a temporary point arrives, and they differ only past the ceiling:
 * <ul>
 *   <li><b>{@linkplain #recoverTemporary Recovered}</b> — "get back what you spent": the session
 *       point, Primor's Rest promise, Uno com a Ira's hours. Refills spent temporaries up to the
 *       ceiling and never beyond.</li>
 *   <li><b>{@linkplain #receiveTemporary Received}</b> — "você receberá N pontos temporários": a
 *       Narrador's grant, Estabilidade Emocional, Destino Favorável. Refills first, exactly like a
 *       recovery; whatever is left over becomes an <b>extra</b> point above the ceiling.</li>
 * </ul>
 * An extra is <strong>consumed, not a ceiling</strong>: it is spent before the ceiling's own points,
 * and once spent it is gone — no recovery ever brings it back. The table's example: Ego 3 full
 * (3 perm / 3 temp) receives 1 → 4 temporary; spends 2 → 2; receives 1 → back to 3 / 3.
 *
 * <p>Extras are held per {@code source}, oldest first, only so that a source can ask what it still
 * holds — Destino Favorável's "não cumulativo" ({@link #receiveNonCumulativeTemporary}) and an
 * Ego loan's "devolvido se não for usado" ({@link #addExtra}/{@link #removeExtras}).
 *
 * <p><b>No Ego is ever above 5.</b> The part of an Ego total past {@link
 * org.aventyrs.core.character.Character#MAX_EGO} is not a wider pool: it is received, once, as extra
 * points, and the Ego counts as 5 everywhere else. {@link #syncOverflow} is how that lands —
 * {@code overflowReceived} remembers how much of the overflow already has, so a Recursos 7 sheet
 * holds 5 + 2 extras on creation and a sixth point earned mid-play is received the moment the
 * total crosses 5, but neither is ever handed over twice.
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
 * and {@code InitiativeEntry#getEffectiveInitiativeValue}. Extras are the one held balance, and
 * rightly: nothing ever clamps them, they only ever go down by being spent.
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
    /** The source every overflow-past-5 extra is held under — see {@link #syncOverflow}. */
    static final Object OVERFLOW = new Object() {
        @Override
        public String toString() {
            return "EgoPointPool.OVERFLOW";
        }
    };

    private final ResourcePool permanent = new ResourcePool();
    private final ResourcePool temporary = new ResourcePool();

    /** Extra temporary points held above the ceiling, per source, oldest first. Never holds a 0. */
    private final Map<Object, Integer> extras = new LinkedHashMap<>();

    /** How much of the Ego's past-5 overflow has already been received — see {@link #syncOverflow}. */
    private int overflowReceived;

    /** Permanent points not yet spent. Floors at 0. */
    int getPermanentRemaining(final int permanentMax) {
        return Math.max(0, permanentMax - permanent.getSpent());
    }

    /**
     * How many temporary points this domain refills up to: permanent points remaining, minus
     * {@code penalty}. Floors at 0. Extras sit above it and are not counted here.
     */
    int getTemporaryCeiling(final int permanentMax, final int penalty) {
        return Math.max(0, getPermanentRemaining(permanentMax) - penalty);
    }

    /** Temporary points not yet spent — the ones under the live ceiling plus every held extra. */
    int getTemporaryRemaining(final int permanentMax, final int penalty) {
        return getCeilingRemaining(permanentMax, penalty) + getExtras();
    }

    /** Every held extra, from every source. */
    int getExtras() {
        return extras.values().stream().mapToInt(Integer::intValue).sum();
    }

    /** The extras source still holds. */
    int getExtras(final Object source) {
        return extras.getOrDefault(source, 0);
    }

    int getOverflowReceived() {
        return overflowReceived;
    }

    /**
     * Spends up to {@code amount} permanent points, returning how many were <em>actually</em>
     * spent — never more than remain. Also lowers the temporary ceiling as a consequence; see
     * this class's own javadoc.
     */
    int spendPermanent(final int permanentMax, final int amount) {
        int spendable = Math.min(Math.max(0, amount), getPermanentRemaining(permanentMax));
        permanent.spend(spendable);
        // The ceiling drops by what was spent; the temporary points still held stay held while they fit
        // under it — see "A permanent spend and the points still held" in this class's javadoc.
        temporary.recover(Math.min(spendable, temporary.getSpent()));
        return spendable;
    }

    /**
     * Spends up to {@code amount} temporary points, extras first (oldest source first), then the
     * ceiling's own — returning how many were <em>actually</em> spent.
     */
    int spendTemporary(final int permanentMax, final int penalty, final int amount) {
        int wanted = Math.max(0, amount);
        int fromExtras = consumeExtras(wanted);
        int fromCeiling = Math.min(wanted - fromExtras, getCeilingRemaining(permanentMax, penalty));
        temporary.spend(fromCeiling);
        return fromExtras + fromCeiling;
    }

    /**
     * Restores up to {@code amount} previously-spent temporary points, returning how many
     * <em>actually</em> came back — bounded by what was spent under the ceiling, so a recovery can
     * never push a pool above its own ceiling, and never gives back a spent extra.
     */
    int recoverTemporary(final int permanentMax, final int penalty, final int amount) {
        clampTemporarySpent(getTemporaryCeiling(permanentMax, penalty));
        int recoverable = Math.min(Math.max(0, amount), temporary.getSpent());
        temporary.recover(recoverable);
        return recoverable;
    }

    /**
     * Receives {@code amount} temporary points from {@code source}: they refill spent temporaries
     * up to the ceiling first, and the remainder is held as extras under {@code source}. Every
     * point lands one way or the other — returns {@code amount} (floored at 0).
     */
    int receiveTemporary(final int permanentMax, final int penalty, final Object source, final int amount) {
        int received = Math.max(0, amount);
        int refilled = recoverTemporary(permanentMax, penalty, received);
        addExtra(source, received - refilled);
        return received;
    }

    /**
     * {@link #receiveTemporary}, but "não cumulativo" — {@code CharismaAbility#DESTINO_FAVORAVEL}'s
     * point: while {@code source} still holds {@code amount} unspent extras, a repeat trigger gives
     * nothing. It tops a partly-spent holding back up to {@code amount}, and it refills first like
     * any received point. Returns how many points were received.
     */
    int receiveNonCumulativeTemporary(final int permanentMax, final int penalty, final Object source,
                                      final int amount) {
        int missing = Math.max(0, amount) - getExtras(source);
        return missing > 0 ? receiveTemporary(permanentMax, penalty, source, missing) : 0;
    }

    /**
     * Holds {@code amount} extras under {@code source} outright, refilling nothing — for points
     * that must stay identifiable until settled (an Ego loan, which goes back to its lender if
     * unused), and for rehydrating a persisted holding.
     */
    void addExtra(final Object source, final int amount) {
        if (amount > 0) {
            extras.merge(source, amount, Integer::sum);
        }
    }

    /** Withdraws every extra {@code source} still holds, returning how many. */
    int removeExtras(final Object source) {
        Integer held = extras.remove(source);
        return held == null ? 0 : held;
    }

    /**
     * Receives whatever part of {@code overflow} — the Ego total past 5 — hasn't been received yet.
     * A lowered overflow (a Talento lost) lowers the mark without clawing back held extras, so
     * crossing 5 again receives again.
     */
    void syncOverflow(final int permanentMax, final int penalty, final int overflow) {
        int owed = overflow - overflowReceived;
        if (owed > 0) {
            receiveTemporary(permanentMax, penalty, OVERFLOW, owed);
        }
        overflowReceived = Math.max(0, overflow);
    }

    /**
     * Replaces the held extras and the overflow mark with persisted values — a consumer rebuilding
     * a sheet, which would otherwise start with its overflow received afresh and its received
     * points lost.
     */
    void restoreExtras(final int heldExtras, final int restoredOverflowReceived) {
        extras.clear();
        addExtra(OVERFLOW, heldExtras);
        overflowReceived = Math.max(0, restoredOverflowReceived);
    }

    private int getCeilingRemaining(final int permanentMax, final int penalty) {
        int ceiling = getTemporaryCeiling(permanentMax, penalty);
        clampTemporarySpent(ceiling);
        return ceiling - temporary.getSpent();
    }

    private int consumeExtras(final int wanted) {
        int consumed = 0;
        Iterator<Map.Entry<Object, Integer>> held = extras.entrySet().iterator();
        while (held.hasNext() && consumed < wanted) {
            Map.Entry<Object, Integer> entry = held.next();
            int taken = Math.min(entry.getValue(), wanted - consumed);
            consumed += taken;
            if (taken == entry.getValue()) {
                held.remove();
            } else {
                entry.setValue(entry.getValue() - taken);
            }
        }
        return consumed;
    }

    /** See "The one rule the formula doesn't give you" in this class's own javadoc. */
    private void clampTemporarySpent(final int ceiling) {
        int excess = temporary.getSpent() - ceiling;
        if (excess > 0) {
            temporary.recover(excess);
        }
    }
}
