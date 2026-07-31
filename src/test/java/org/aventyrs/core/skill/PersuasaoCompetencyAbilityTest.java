package org.aventyrs.core.skill;

import org.aventyrs.core.modifier.ModifierResolver;
import org.aventyrs.core.modifier.ModifierResolverImpl;
import org.aventyrs.core.modifier.ModifierType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class PersuasaoCompetencyAbilityTest {

    @Test
    void everyAbilityBelongsToPersuasao() {
        for (PersuasaoCompetencyAbility ability : PersuasaoCompetencyAbility.values()) {
            assertEquals(SkillType.PERSUASAO, ability.getSkillType());
        }
    }

    @Test
    void everyAbilityHasADescription() {
        for (PersuasaoCompetencyAbility ability : PersuasaoCompetencyAbility.values()) {
            assertFalse(ability.getDescription().isBlank());
        }
    }

    @Test
    void listHasTheFiveDescribedAbilities() {
        assertEquals(5, PersuasaoCompetencyAbility.values().length);
    }

    @Test
    void noAbilityReducesDifficulty() {
        for (PersuasaoCompetencyAbility ability : PersuasaoCompetencyAbility.values()) {
            assertEquals(0, ability.getDifficultyReduction());
        }
    }

    @Test
    void onlyPokerfaceGrantsASkillRollBonus() {
        ModifierResolver modifierResolver = new ModifierResolverImpl();
        for (PersuasaoCompetencyAbility ability : PersuasaoCompetencyAbility.values()) {
            int expected = ability == PersuasaoCompetencyAbility.POKERFACE ? Skill.ADVANTAGE_BONUS : 0;
            assertEquals(expected, modifierResolver.sumModifiers(ability, ModifierType.SKILL_ROLL_BONUS));
        }
    }
}
