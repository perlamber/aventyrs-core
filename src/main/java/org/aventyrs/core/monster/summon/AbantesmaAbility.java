package org.aventyrs.core.monster.summon;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.aventyrs.core.character.MovementMode;
import org.aventyrs.core.modifier.Modifier;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.skill.SkillType;

/**
 * The Abantesma's Características Especiais (core 0.1.6.25) — the {@link ZumbiAbility} shape for a ranged attacker: its
 * Conjurador's Graduação on Ataque à Distância, one nível off every Perícia at 10, and "Movimento Base de Voo".
 */
@Getter
@RequiredArgsConstructor
@EqualsAndHashCode
public class AbantesmaAbility implements SkillCompetencyAbility {

    private final int conjuradorManaGraduation;

    @Modifier(ModifierType.ATAQUE_A_DISTANCIA_ROLL_BONUS)
    public int conjuradorAttackBonus() {
        return Math.max(0, conjuradorManaGraduation);
    }

    @Override
    public int getDifficultyReduction() {
        return conjuradorManaGraduation == ZumbiAbility.DIFFICULTY_REDUCTION_TIER ? 1 : 0;
    }

    /** "Possuem Movimento Base de Voo." */
    @Override
    public boolean grantsMovementMode(final MovementMode mode) {
        return mode == MovementMode.FLIGHT;
    }

    @Override
    public SkillType getSkillType() {
        return SkillType.ATAQUE_A_DISTANCIA;
    }

    @Override
    public String getDescription() {
        return "Recebem Bônus em Perícia de Ataque igual à quantidade de Graduações em Domínio do Mana de seu "
                + "Conjurador. Possuem Movimento Base de Voo.";
    }
}
