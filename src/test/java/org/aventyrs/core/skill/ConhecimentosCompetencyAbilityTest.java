package org.aventyrs.core.skill;

import org.aventyrs.core.modifier.ModifierResolver;
import org.aventyrs.core.modifier.ModifierResolverImpl;
import org.aventyrs.core.modifier.ModifierType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ConhecimentosCompetencyAbilityTest {

    @Test
    void everyAbilityBelongsToConhecimentos() {
        for (ConhecimentosCompetencyAbility ability : ConhecimentosCompetencyAbility.values()) {
            assertEquals(SkillType.CONHECIMENTOS, ability.getSkillType());
        }
    }

    @Test
    void everyAbilityHasADescription() {
        for (ConhecimentosCompetencyAbility ability : ConhecimentosCompetencyAbility.values()) {
            assertFalse(ability.getDescription().isBlank());
        }
    }

    @Test
    void listHasTheFiveDescribedAbilities() {
        assertEquals(5, ConhecimentosCompetencyAbility.values().length);
    }

    @Test
    void noAbilityReducesDifficulty() {
        for (ConhecimentosCompetencyAbility ability : ConhecimentosCompetencyAbility.values()) {
            assertEquals(0, ability.getDifficultyReduction());
        }
    }

    @Test
    void onlyOProfessorGrantsASkillRollBonus() {
        ModifierResolver modifierResolver = new ModifierResolverImpl();
        for (ConhecimentosCompetencyAbility ability : ConhecimentosCompetencyAbility.values()) {
            int expected = ability == ConhecimentosCompetencyAbility.O_PROFESSOR ? Skill.ADVANTAGE_BONUS : 0;
            assertEquals(expected, modifierResolver.sumModifiers(ability, ModifierType.SKILL_ROLL_BONUS));
        }
    }
}
