package org.aventyrs.core.race;

import org.aventyrs.core.modifier.ModifierResolver;
import org.aventyrs.core.modifier.ModifierResolverImpl;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AvianosRacialAbilityTest {

    private final ModifierResolver modifierResolver = new ModifierResolverImpl();

    @Test
    void everyAbilityHasADescription() {
        for (AvianosRacialAbility ability : AvianosRacialAbility.values()) {
            assertFalse(ability.getDescription().isBlank());
        }
    }

    @Test
    void visaoAlemDoAlcanceBelongsToAttention() {
        assertEquals(SkillType.ATTENTION, AvianosRacialAbility.VISAO_ALEM_DO_ALCANCE.getSkillType());
    }

    @Test
    void visaoAlemDoAlcanceGrantsAFlatAttentionRollBonus() {
        assertEquals(Skill.ADVANTAGE_BONUS, modifierResolver.sumModifiers(
                AvianosRacialAbility.VISAO_ALEM_DO_ALCANCE, ModifierType.ATTENTION_ROLL_BONUS));
    }

    @Test
    void visaoAlemDoAlcanceDoesNotLeakIntoAnotherPericiasRollBonus() {
        assertEquals(0, modifierResolver.sumModifiers(
                AvianosRacialAbility.VISAO_ALEM_DO_ALCANCE, ModifierType.FURTIVIDADE_ROLL_BONUS));
        assertEquals(0, modifierResolver.sumModifiers(
                AvianosRacialAbility.VISAO_ALEM_DO_ALCANCE, ModifierType.SKILL_ROLL_BONUS));
    }
}
