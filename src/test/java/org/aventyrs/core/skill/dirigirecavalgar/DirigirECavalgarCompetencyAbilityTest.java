package org.aventyrs.core.skill.dirigirecavalgar;

import org.aventyrs.core.modifier.ModifierResolver;
import org.aventyrs.core.modifier.ModifierResolverImpl;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class DirigirECavalgarCompetencyAbilityTest {

    @Test
    void everyAbilityBelongsToDirigirECavalgar() {
        for (DirigirECavalgarCompetencyAbility ability : DirigirECavalgarCompetencyAbility.values()) {
            assertEquals(SkillType.DIRIGIR_E_CAVALGAR, ability.getSkillType());
        }
    }

    @Test
    void everyAbilityHasADescription() {
        for (DirigirECavalgarCompetencyAbility ability : DirigirECavalgarCompetencyAbility.values()) {
            assertFalse(ability.getDescription().isBlank());
        }
    }

    @Test
    void listHasTheFiveDescribedAbilities() {
        assertEquals(5, DirigirECavalgarCompetencyAbility.values().length);
    }

    @Test
    void noAbilityReducesDifficulty() {
        for (DirigirECavalgarCompetencyAbility ability : DirigirECavalgarCompetencyAbility.values()) {
            assertEquals(0, ability.getDifficultyReduction());
        }
    }

    @Test
    void onlyControlarAnimaisGrantsASkillRollBonus() {
        ModifierResolver modifierResolver = new ModifierResolverImpl();
        for (DirigirECavalgarCompetencyAbility ability : DirigirECavalgarCompetencyAbility.values()) {
            int expected = ability == DirigirECavalgarCompetencyAbility.CONTROLAR_ANIMAIS ? Skill.ADVANTAGE_BONUS : 0;
            assertEquals(expected, modifierResolver.sumModifiers(ability, ModifierType.SKILL_ROLL_BONUS));
        }
    }
}
