package org.aventyrs.core.character.services;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.sheet.Blessing;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.EgoPointSpend;
import org.aventyrs.core.sheet.EgoPointType;

import java.util.List;
import java.util.Map;

/**
 * Per-game-session recovery of temporary Ego points — the one recovery path that isn't a
 * specific promise made by a particular effect (that's {@code
 * org.aventyrs.core.sheet.PendingEgoRecovery}, which {@code RestService} resolves).
 *
 * <p>A {@code Character} recovers exactly {@link #SESSION_TEMPORARY_RECOVERY} temporary Ego
 * point per game session — <strong>one in total, across all four {@link EgoDomain}s, not one
 * per domain</strong> — and the recovering player chooses which Ego receives it, which is why
 * {@link #applySessionRecovery} takes that choice as a parameter rather than spreading points
 * around itself. A Vantagem de Ego may grant extras on top, in its own domain; see {@code
 * org.aventyrs.core.ego.EgoAdvantage#resolveExtraSessionEgoRecovery}.
 *
 * <p><strong>Permanent Ego points are not recovered here, or anywhere.</strong> They are only
 * ever <em>earned</em>, through {@code AttributeAbility#resolvePermanentEgoGain}. Nothing in
 * this ruleset gives a spent one back.
 *
 * <p><strong>A Rest does not refill the temporary pool either</strong> — {@code
 * RestService#applyRest} deliberately doesn't call into this service. Temporary Ego recovery is
 * scoped to a *session*, not a Descanso, and the one Rest-scoped Ego path that does exist is
 * Primor's own separate promise.
 *
 * <p><strong>The trigger is a GM action in the consuming app, not anything in here.</strong>
 * Nothing calls {@link #applySessionRecovery} automatically, and nothing should: a game session
 * ends when the people at the table say it does, which is a judgement no rules engine can make.
 * The intended shape is a Narrador pressing an end-of-session button, which maps onto {@link
 * #applySessionRecovery(Map)} — one call, carrying the table's collected choices.
 *
 * <p>There is still no session <em>identity</em> and no counter here, so recovery through this
 * service is deliberately not idempotent (see {@link #applySessionRecovery(Map)}) — a manual
 * button marks a boundary without ever telling this core it was crossed.
 *
 * <p>A clause that must fire only <em>once</em> within a session is a different question, and it
 * <strong>is</strong> answered: {@code CombatantSheet#consumeOncePerSession} claims a marker
 * against transient per-sheet state, where a session is the sheet object's own lifetime in the
 * running client ({@code GnoseAbility#ESTABILIDADE_EMOCIONAL} is its first consumer; {@code
 * MeioElfo}'s "1x por sessão" is blocked on other pieces, not on this one). That guard
 * deliberately does not depend on anyone pressing the end-of-session button this service serves
 * — the two are independent notions of a session, and only one of them is a UI action.
 */
public interface EgoPointsService {

    /**
     * The baseline temporary Ego point recovered per game session — one, in total, across every
     * {@link EgoDomain}, not one apiece.
     */
    int SESSION_TEMPORARY_RECOVERY = 1;

    /**
     * Extra temporary points character's Vantagem de Ego in domain (if any) recovers per session,
     * beyond {@link #SESSION_TEMPORARY_RECOVERY}. Zero when no Vantagem is held there, or when
     * the one held grants none.
     */
    int getExtraSessionRecovery(Character character, EgoDomain domain);

    /**
     * Applies one game session's worth of temporary Ego recovery to sheet: {@link
     * #SESSION_TEMPORARY_RECOVERY} into chosenDomain — the player's own choice — plus, for every
     * domain independently, whatever {@link #getExtraSessionRecovery} that domain's held Vantagem
     * grants.
     *
     * <p>Every recovery is bounded by its domain's own ceiling, so this can never push a pool
     * past what it may hold. Returns {@code void}, mirroring {@code RestService#applyRest}: the
     * sheet's own readers report the outcome.
     */
    void applySessionRecovery(CombatantSheet sheet, EgoDomain chosenDomain);

    /**
     * Applies one game session's worth of temporary Ego recovery to every sheet in
     * chosenDomains, each into that sheet's own mapped {@link EgoDomain} — the bulk form, and
     * the entry point a Narrador's end-of-session button maps onto: one call per click, carrying
     * the whole table's collected choices.
     *
     * <p><strong>The map is the entire selection.</strong> Only sheets present are recovered,
     * and that is precisely how a consumer excludes a foe on the field or a player who wasn't in
     * the Scene. There is deliberately no {@code instanceof CharacterSheet} filter and no
     * "everyone in the Scene" default: a monster recovering between sessions is meaningless but
     * harmless, and <em>which</em> combatants are the party is the consumer's knowledge, never
     * this core's. Pair it with {@code Scene#getAllParticipants()} to build the choice UI, then
     * pass back only the entries that should recover.
     *
     * <p>The per-player choice lives in the map because the rules put it there — each player
     * picks which of their Egos receives the single baseline point. A Vantagem's extras ignore
     * that choice and land in the Vantagem's own domain regardless, exactly as in the
     * single-sheet form this delegates to.
     *
     * <p><strong>Not a cascading overload.</strong> This delegates <em>outward</em>, to one
     * {@link #applySessionRecovery(CombatantSheet, EgoDomain)} call per entry — it is not a
     * longer overload holding the real logic with shorter ones passing {@code null}, which is
     * the convention elsewhere in this codebase. Don't restructure it into that shape.
     *
     * <p><strong>Deliberately not idempotent.</strong> Calling this twice recovers twice,
     * bounded only by each pool's own ceiling — so a character who had spent two or more
     * temporary points genuinely gains a second one from a second click. Guarding that is the
     * consuming app's job, because this core has no session boundary to hang a guard on and
     * inventing one here would be exactly the session concept it deliberately lacks. A consumer
     * should persist its own session identity plus the set of sheet ids already recovered, and
     * disable the button once pressed.
     */
    void applySessionRecovery(Map<CombatantSheet, EgoDomain> chosenDomains);

    /** The lowest face a d6 can show — {@code rolledValue}'s lower bound. */
    int MIN_DIE_FACE = 1;

    /** The highest face a d6 can show — {@code rolledValue}'s upper bound. */
    int MAX_DIE_FACE = 6;

    /**
     * How much PV, PM and PD a completed spend additionally recovers, from the Vantagem de Ego
     * held in that spend's <em>own</em> {@link EgoDomain} — e.g. {@code
     * org.aventyrs.core.ego.AutocontroleAdvantage#DETERMINACAO_HEROICA}'s "+1d6PV, PM e PD",
     * doubled when the point spent was permanent. One figure, applied to all three pools alike.
     * Zero when no Vantagem is held in that domain, when the one held doesn't react to spends,
     * or when the spend took no points at all.
     *
     * <p>{@code rolledValue} is the caller's already-rolled die: this core never rolls dice.
     * Validated as a legal d6 face ({@link #MIN_DIE_FACE}..{@link #MAX_DIE_FACE}), a genuine
     * system boundary the same way {@code SkillRoll}'s own dice validation is — and one worth
     * enforcing here, because a negative would otherwise reach {@code CombatantSheet#heal} and
     * silently <em>damage</em> the character instead of healing them.
     *
     * <p>A pure computation — it recovers nothing itself. {@link #useEgoPointsForEffect} is what
     * applies it.
     */
    int getSpendRecovery(Character character, EgoPointSpend spend, int rolledValue);

    /**
     * Deliberately uses amount Ego points of type from domain for some effect, then applies
     * whatever {@link #getSpendRecovery} that use earns to sheet's PV, PM and PD. Returns the
     * completed {@link EgoPointSpend}, so a caller can see which pool was drawn from and how
     * many points actually left it.
     *
     * <p><strong>This is the "usar pontos" path, and the drain path is deliberately separate.</strong>
     * {@code CombatantSheet#spendEgoPoints} also serves {@code org.aventyrs.core.effect.Primor}
     * taking points from a victim against their will, and a Vantagem reacting to <em>that</em>
     * would mean a critical hit healing the character it just hit. Routing a holder's own
     * chosen expenditure through here is what tells the two apart — there is no flag on the
     * spend itself, and no observer watching the sheet.
     *
     * <p>Spending and recovering in one call, rather than leaving the caller to apply the
     * recovery after a bare spend, is deliberate: a separate second call is one a caller can
     * forget, and the recovery would then silently never happen.
     *
     * <p>What this does <em>not</em> do is decide what the points were spent <em>on</em> —
     * "qualquer efeito" is the caller's own business, and this core has no notion of the effect
     * being paid for. It only resolves the recovery that accompanies the expenditure.
     */
    /**
     * Every {@link Blessing} a completed spend earns, from the Vantagem de Ego held in that
     * spend's <em>own</em> {@link EgoDomain} — e.g. {@code
     * org.aventyrs.core.ego.SorteAdvantage#AS_NA_MANGA}'s 2UD of movement off a Ponto de Sorte.
     * Empty when no Vantagem is held there, when the one held grants none, or when the spend
     * took no points at all.
     *
     * <p>A pure computation — it grants nothing itself. {@link #useEgoPointsForEffect} is what
     * applies these, directly to the spender.
     */
    List<Blessing> getSpendBlessings(Character character, EgoPointSpend spend);

    EgoPointSpend useEgoPointsForEffect(CombatantSheet sheet, EgoDomain domain, EgoPointType type,
                                        int amount, int rolledValue);
}
