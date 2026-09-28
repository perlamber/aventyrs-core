package org.aventyrs.core.sheet;

import org.aventyrs.core.skill.SkillType;

/**
 * The 1d6 a Cego combatant throws beside every Perícia roll — {@link ConditionType#CEGO}: "Deve
 * rolar 1d6 sempre que efetuar uma rolagem de perícia. Perícias de efeitos pessoal, falham com
 * resultados 2 ou menos. Rolagens de Ataque corpo a Corpo falham com resultados 3 ou menos. Rolagens
 * de Ataque à Distância falham com resultados menores que 5."
 *
 * <p>Only the table: <b>whether</b> a roller must throw it is {@link
 * CombatantSheet#getBlindCheckThreshold} (Cego, and not exempted by a held Talento — {@code
 * DuelistaFeat#COMBATER_AS_CEGAS}), and the die itself rides the roll ({@code
 * SkillRoll#withBlindCheck}). ⚠️ Every Perícia that is not an attack is read as "de efeito pessoal":
 * the text names three kinds of roll and no other, and a Perícia test is the roller's own effect.
 */
public final class BlindCheck {

    /** "Perícias de efeitos pessoal, falham com resultados 2 ou menos." */
    public static final int PERSONAL_EFFECT_FAILURE = 2;

    /** "Rolagens de Ataque corpo a Corpo falham com resultados 3 ou menos." */
    public static final int MELEE_ATTACK_FAILURE = 3;

    /** "Rolagens de Ataque à Distância falham com resultados menores que 5" — 4 or less. */
    public static final int RANGED_ATTACK_FAILURE = 4;

    private BlindCheck() {
    }

    /** The highest d6 face that fails a skillType roll made blind. */
    public static int failureThresholdFor(final SkillType skillType) {
        if (skillType == SkillType.ATAQUE_CORPO_A_CORPO) {
            return MELEE_ATTACK_FAILURE;
        }
        if (skillType == SkillType.ATAQUE_A_DISTANCIA) {
            return RANGED_ATTACK_FAILURE;
        }
        return PERSONAL_EFFECT_FAILURE;
    }

    /** Whether face fails a skillType roll made blind. */
    public static boolean fails(final SkillType skillType, final int face) {
        return face <= failureThresholdFor(skillType);
    }
}
