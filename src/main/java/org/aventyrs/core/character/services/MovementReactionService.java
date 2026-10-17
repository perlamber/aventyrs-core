package org.aventyrs.core.character.services;

import org.aventyrs.core.action.Manoeuvre;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CombatantSheet;

import java.util.List;

/**
 * Who gets a Reação when a combatant moves.
 *
 * <p>The rule: <b>moving provokes a Reação from every enemy holding a drawn melee weapon who has
 * the mover within their own attack range.</b> An Ataque à Distância never provokes one — a
 * bow-armed enemy is not a threat under this rule, however close they are — and a manoeuvre or
 * Habilidade may exempt the movement outright ({@code DexterityAbility#IMPLACAVEL}).
 *
 * <p>It is a <b>general movement rule</b>, not an Investida one, which is why it lives here rather
 * than on {@code ChargeService} — the charge is merely the first manoeuvre modelled that triggers
 * it. {@code ChargeService#begin} delegates to this service rather than reimplementing it.
 *
 * <p>Kept off {@link MovementService} on purpose: that interface answers one question, "how far
 * does one Ponto de Ação carry this combatant", and its whole contract is built around that figure
 * being per-Ponto-de-Ação. This is a different question about the same act, and it takes the
 * one-method shape {@link AttackTargetingService} and {@link DefeatBlessingService} already use.
 *
 * <h2>Three things it deliberately does not do</h2>
 *
 * <ul>
 *   <li><b>Nothing fires a Reação.</b> This core has no mechanism for one: {@link ReactionsService}
 *   computes a combatant's <em>maximum</em> and nothing anywhere tracks how many they have spent.
 *   So the returned list is the opportunity, not the act, and a reactor's remaining Reações are
 *   deliberately not consulted — the caller adjudicates. Real, exact, unapplied data, the same
 *   discipline {@code org.aventyrs.core.combat.AttackDelivery}'s {@code
 *   unappliedDifficultyReduction} follows.</li>
 *   <li><b>It answers about where the mover is <em>now</em>.</b> A movement's path and distance are
 *   not recorded anywhere in this core — {@code CombatantSheet#consumeMovementThisRound()} counts
 *   movements and nothing else — so a caller that cares about ground crossed asks once per step
 *   rather than expecting one call to sweep a route.</li>
 *   <li><b>It judges one snapshot</b>, not the ground a charge crosses: an Investida names its target
 *   first and then moves as one atomic action, so a caller wanting every reactor along the path asks
 *   again from each step.</li>
 * </ul>
 *
 * <h2>Defender o Perímetro, and the charge's target</h2>
 *
 * The Reação to an enemy moving inside your melee reach is <b>Defender o Perímetro</b>. Table ruling
 * (2026-09-28): an Investida names its target <em>before</em> it moves, and <b>that target cannot
 * Defender o Perímetro against the charger</b> — everyone else within reach can. A held Talento lifts
 * this ({@code Feat#permitsPerimeterDefenceAsChargeTarget} — {@code EscudeiroFeat#MESTRE_ESCUDEIRO},
 * "mesmo quando for alvo de investidas", while using a Escudo). The four-argument form names the target.
 */
public interface MovementReactionService {

    /**
     * The enemies entitled to react to mover's movement, in the order {@code
     * SceneContext#getEnemies()} lists them, never {@code null}.
     *
     * <p>sceneContext is the <b>mover's own</b> snapshot — the correct one to read here, unlike the
     * attacker's snapshot {@code Feat#resolveCriticalResistance} is warned about, because the
     * distances on it are measured from the mover and distance between two combatants is mutual.
     * {@code null} yields an empty list: with no positions known, "cannot tell" reads as no
     * opportunity rather than as every enemy reacting.
     *
     * <p>manoeuvre names what kind of movement this is ({@link Manoeuvre#INVESTIDA}), or {@code
     * null} for an ordinary one. It is passed through to {@code
     * AttributeAbility#exemptsFromMovementReactions} unchanged, since every authored exemption is
     * scoped to a particular manoeuvre rather than to movement at large.
     */
    default List<CombatantSheet> getProvokedReactors(CombatantSheet mover, SceneContext sceneContext,
                                                     Manoeuvre manoeuvre) {
        return getProvokedReactors(mover, sceneContext, manoeuvre, null);
    }

    /**
     * Same as the three-argument form, for an Investida aimed at chargeTarget: the target is left out
     * — the charge's target cannot Defender o Perímetro — unless a Talento it holds lets it react
     * anyway. chargeTarget is ignored for any other manoeuvre, and {@code null} names nobody.
     */
    default List<CombatantSheet> getProvokedReactors(CombatantSheet mover, SceneContext sceneContext,
                                                     Manoeuvre manoeuvre, CombatantSheet chargeTarget) {
        return getProvokedReactors(mover, sceneContext, manoeuvre, chargeTarget, null);
    }

    /**
     * Same as the four-argument form, judged where a movement starts: progress names which movement of the Rodada
     * this is, so a window over its first hexes ({@code ActionProfile#resolveReactionFreeSteps}) that has not yet
     * closed exempts the snapshot. {@code null} is "not told", which exempts nothing.
     */
    List<CombatantSheet> getProvokedReactors(CombatantSheet mover, SceneContext sceneContext, Manoeuvre manoeuvre,
                                             CombatantSheet chargeTarget,
                                             org.aventyrs.core.action.MovementProgress progress);

    /**
     * Where along route the movement provokes Defender o Perímetro, and from whom (core 0.1.6.17; table ruling
     * 2026-10-14). route lists the mover's positions in order, its start first. Each hostile of moverContext with a
     * position in positions reacts at the first position its melee reach covers — the start included, since leaving
     * it is moving within reach — and at no later one. Distance is three-dimensional ({@code HexGrid#distance3d}) less
     * both bodies' radii.
     *
     * <p>The same exemptions as {@link #getProvokedReactors} apply: Reposicionar provokes nothing, a held exemption
     * none, and an Investida's own target does not react unless a Talento lets it. A {@code null} moverContext is
     * nobody. Empty when nobody reacts; checkpoints in route order.
     */
    java.util.List<org.aventyrs.core.action.ReactionCheckpoint> getRouteCheckpoints(
            CombatantSheet mover, java.util.List<org.aventyrs.core.scene.grid.BoardPosition> route, Manoeuvre manoeuvre,
            SceneContext moverContext,
            java.util.Map<CombatantSheet, org.aventyrs.core.scene.grid.BoardPosition> positions,
            CombatantSheet chargeTarget);

    /**
     * The same, with some hostiles' melee reach already known in UD (body radius included) — a client's stand-in for a
     * combatant another client owns carries no weapons, so its owner advertises the reach and the mover's client passes
     * it here. A hostile in knownReachUd threatens a position whose gap is within that figure; any other is judged by
     * the weapons on its sheet.
     */
    default java.util.List<org.aventyrs.core.action.ReactionCheckpoint> getRouteCheckpoints(
            CombatantSheet mover, java.util.List<org.aventyrs.core.scene.grid.BoardPosition> route, Manoeuvre manoeuvre,
            SceneContext moverContext,
            java.util.Map<CombatantSheet, org.aventyrs.core.scene.grid.BoardPosition> positions,
            CombatantSheet chargeTarget, java.util.Map<CombatantSheet, Integer> knownReachUd) {
        return getRouteCheckpoints(mover, route, manoeuvre, moverContext, positions, chargeTarget, knownReachUd, null);
    }

    /**
     * The same, told where the route begins among the mover's movements this Rodada, so a window over a movement's
     * first hexes applies ({@code ActionProfile#resolveReactionFreeSteps} — Consciência Defensiva's first 2UD of the
     * first movement; table rulings 2026-10-16). Step k of the route goes from position k-1 to position k, and the
     * first f steps still inside the window provoke nothing. A position provokes for the step into it or the step
     * out of it, so a hostile reacts at the first position its reach covers from index f on — the mover pausing
     * there before its first unexempt step — and a route ending at index f, or before it, provokes nothing in the
     * window. A hostile whose reach covered only exempt positions does not react. {@code null} progress is "not
     * told", which opens no window.
     */
    java.util.List<org.aventyrs.core.action.ReactionCheckpoint> getRouteCheckpoints(
            CombatantSheet mover, java.util.List<org.aventyrs.core.scene.grid.BoardPosition> route, Manoeuvre manoeuvre,
            SceneContext moverContext,
            java.util.Map<CombatantSheet, org.aventyrs.core.scene.grid.BoardPosition> positions,
            CombatantSheet chargeTarget, java.util.Map<CombatantSheet, Integer> knownReachUd,
            org.aventyrs.core.action.MovementProgress progress);

    /**
     * The hostiles whose melee reach covers subject where it stands — no movement involved, so no movement exemption
     * applies (a Reposicionar, Implacável, Posicionamento Estratégico, Consciência Defensiva). What an action taken
     * in place asks: Impedir Conjuração's {@code ENEMY_CASTS_IN_REACH}. Same reach rules and knownReachUd as
     * {@link #getRouteCheckpoints}; empty with no context.
     */
    List<CombatantSheet> getHostilesInReach(CombatantSheet subject, org.aventyrs.core.scene.grid.BoardPosition at,
                                            SceneContext subjectContext,
                                            java.util.Map<CombatantSheet, org.aventyrs.core.scene.grid.BoardPosition> positions,
                                            java.util.Map<CombatantSheet, Integer> knownReachUd);

    /**
     * How far sheet's melee reach extends, in UD, from the weapons it holds ready — drawn ones and Armas Naturais
     * swung as an Ataque Corpo-a-Corpo — or empty with none in hand. What a client advertises for a combatant it owns.
     */
    java.util.OptionalInt getMeleeReachUd(CombatantSheet sheet);
}
