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
    List<CombatantSheet> getProvokedReactors(CombatantSheet mover, SceneContext sceneContext, Manoeuvre manoeuvre,
                                             CombatantSheet chargeTarget);
}
