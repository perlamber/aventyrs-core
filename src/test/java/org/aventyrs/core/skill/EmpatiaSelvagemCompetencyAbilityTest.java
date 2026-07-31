package org.aventyrs.core.skill;

import org.aventyrs.core.modifier.ModifierResolver;
import org.aventyrs.core.modifier.ModifierResolverImpl;
import org.aventyrs.core.modifier.ModifierType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class EmpatiaSelvagemCompetencyAbilityTest {

    @Test
    void everyAbilityBelongsToEmpatiaSelvagem() {
        for (EmpatiaSelvagemCompetencyAbility ability : EmpatiaSelvagemCompetencyAbility.values()) {
            assertEquals(SkillType.EMPATIA_SELVAGEM, ability.getSkillType());
        }
    }

    @Test
    void everyAbilityHasADescription() {
        for (EmpatiaSelvagemCompetencyAbility ability : EmpatiaSelvagemCompetencyAbility.values()) {
            assertFalse(ability.getDescription().isBlank());
        }
    }

    @Test
    void listHasTheFiveDescribedAbilities() {
        assertEquals(5, EmpatiaSelvagemCompetencyAbility.values().length);
    }

    @Test
    void noAbilityReducesDifficulty() {
        for (EmpatiaSelvagemCompetencyAbility ability : EmpatiaSelvagemCompetencyAbility.values()) {
            assertEquals(0, ability.getDifficultyReduction());
        }
    }

    @Test
    void onlyAmainarASelvageriaGrantsASkillRollBonus() {
        ModifierResolver modifierResolver = new ModifierResolverImpl();
        for (EmpatiaSelvagemCompetencyAbility ability : EmpatiaSelvagemCompetencyAbility.values()) {
            int expected = ability == EmpatiaSelvagemCompetencyAbility.AMAINAR_A_SELVAGERIA ? Skill.ADVANTAGE_BONUS : 0;
            assertEquals(expected, modifierResolver.sumModifiers(ability, ModifierType.SKILL_ROLL_BONUS));
        }
    }
}
