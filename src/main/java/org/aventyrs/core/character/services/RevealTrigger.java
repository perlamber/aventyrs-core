package org.aventyrs.core.character.services;

import org.aventyrs.core.sheet.ConditionType;

/**
 * Something a hidden character did that the rules say gives them away — the argument to {@code
 * HidingService#reveals}/{@code HidingService#reveal}.
 *
 * <p>An enum rather than a method per trigger because the three differ only in <i>whether a
 * Talento can excuse them</i>, not in what they do: each one ends {@link
 * ConditionType#ESCONDIDO} unless excused. A fourth kind of give-away is a constant here, not a
 * new method on the service.
 *
 * <p><b>Which constant applies is the caller's to decide</b>, and for one of them that is not a
 * formality: this core never records who a Perícia roll was aimed at ({@code CombatantAction}
 * carries the Perícia, the governing Atributo and the {@code AttackSource}, but no target), so
 * nothing here can tell a self-buff from a roll made against somebody else. The caller resolving
 * the roll passed that target into {@code AbstractSkillInteraction#applyTo} and knows.
 */
public enum RevealTrigger {

    /**
     * "Um ataque de qualquer tipo" — an Ataque Corpo-a-Corpo, an Ataque à Distância, or a Magia
     * that deals damage. Always reveals; no Talento excuses it.
     *
     * <p>A damaging Magia is deliberately not a constant of its own. The rules name it separately
     * ("ou conjura uma magia com danos") because a Magia is not obviously an attack, but the
     * consequence is identical, and a non-damaging Magia aimed at somebody else is already {@link
     * #SKILL_ROLL_ON_ANOTHER}.
     */
    ATTACK,

    /**
     * Any Perícia roll made against another combatant. A roll aimed at oneself — a self-buff, a
     * Conhecimentos check, the Furtividade roll that did the hiding in the first place — is not
     * this, and a caller simply does not report it.
     */
    SKILL_ROLL_ON_ANOTHER,

    /**
     * Moving. Excused by {@code MobilidadeFeat#MOVIMENTO_FURTIVO} ("você pode se mover enquanto
     * furtivo") through {@code Feat#movesWhileHidden} — the only excused trigger of the three.
     *
     * <p>One movement, not a distance: this core records that a combatant moved ({@code
     * CombatantSheet#consumeMovementThisRound()}) and never how far, so a caller reports the act.
     */
    MOVEMENT
}
