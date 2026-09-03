package org.aventyrs.core.sheet;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.skill.AttackSource;
import org.aventyrs.core.skill.SkillType;

/**
 * One action a combatant took this Rodada — appended to {@link CombatantSheet#getActionsThisRound()}
 * by the API via {@link CombatantSheet#recordAction}, and cleared at the Rodada boundary by
 * {@link CombatantSheet#startNewRound()}. The log is what lets a clause ask "was a qualifying
 * action already taken this Rodada / this Turn" — {@code AssassinoFeat#SAQUE_RELAMPAGO}'s
 * "a primeira vez que fizer um ataque... a cada Rodada", and {@code CombatantSheet
 * #isFirstRollOfTurnFor} for {@code DexterityAbility#PRECISAO}.
 *
 * <p>Scope: a roll-action only for now (an attack or a Perícia check). Movement has its own
 * counter ({@code consumeMovementThisRound}); ability activations and roll-less casts are not
 * logged here yet.
 *
 * @param skill          the Perícia rolled.
 * @param governingDomain the {@link AttributeDomain} that actually governed the roll <em>after</em>
 *                       every substitution (Perito Teórico / {@code SkillCompetencyAbility} /
 *                       {@link AttackSource}) — read back from {@code
 *                       InteractionResult#getGoverningAttributeDomain()}, not re-derived.
 * @param attackSource   what the attack was delivered with, or {@code null}. First-consumer-shaped:
 *                       only {@code SAQUE_RELAMPAGO} reads it (a ranged weapon and a ranged spell
 *                       share {@link SkillType#ATAQUE_A_DISTANCIA}, so {@code skill} alone can't
 *                       tell its chosen Armas/Magias apart).
 * @param cost           what the action cost — see {@link ActionCost}.
 * @param turnNumber     the Rodada this action was taken in (the value {@code Scene#getCurrentRound()}
 *                       reported), carried for the API's own history.
 * @param outcome        the roll's verdict, or {@code null} — see {@link ActionOutcome}.
 */
public record CombatantAction(SkillType skill, AttributeDomain governingDomain,
                              AttackSource attackSource, ActionCost cost,
                              int turnNumber, ActionOutcome outcome) {
}
