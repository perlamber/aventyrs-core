package org.aventyrs.core.skill;

import org.aventyrs.core.modifier.ModifierResolver;
import org.aventyrs.core.modifier.ModifierResolverImpl;
import org.aventyrs.core.modifier.ModifierType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class MedicinaECuraCompetencyAbilityTest {

    @Test
    void everyAbilityBelongsToMedicinaECura() {
        for (MedicinaECuraCompetencyAbility ability : MedicinaECuraCompetencyAbility.values()) {
            assertEquals(SkillType.MEDICINA_E_CURA, ability.getSkillType());
        }
    }

    @Test
    void everyAbilityHasADescription() {
        for (MedicinaECuraCompetencyAbility ability : MedicinaECuraCompetencyAbility.values()) {
            assertFalse(ability.getDescription().isBlank());
        }
    }

    @Test
    void listHasTheFiveDescribedAbilities() {
        assertEquals(5, MedicinaECuraCompetencyAbility.values().length);
    }

    @Test
    void noAbilityReducesDifficultyOrGrantsASkillRollBonusYet() {
        ModifierResolver modifierResolver = new ModifierResolverImpl();
        for (MedicinaECuraCompetencyAbility ability : MedicinaECuraCompetencyAbility.values()) {
            assertEquals(0, ability.getDifficultyReduction());
            assertEquals(0, modifierResolver.sumModifiers(ability, ModifierType.SKILL_ROLL_BONUS));
        }
    }
}
