package org.aventyrs.core.skill;

import org.aventyrs.core.modifier.ModifierResolver;
import org.aventyrs.core.modifier.ModifierResolverImpl;
import org.aventyrs.core.modifier.ModifierType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AtaqueCorpoACorpoCompetencyAbilityTest {

    @Test
    void everyAbilityBelongsToAtaqueCorpoACorpo() {
        for (AtaqueCorpoACorpoCompetencyAbility ability : AtaqueCorpoACorpoCompetencyAbility.values()) {
            assertEquals(SkillType.ATAQUE_CORPO_A_CORPO, ability.getSkillType());
        }
    }

    @Test
    void everyAbilityHasADescription() {
        for (AtaqueCorpoACorpoCompetencyAbility ability : AtaqueCorpoACorpoCompetencyAbility.values()) {
            assertFalse(ability.getDescription().isBlank());
        }
    }

    @Test
    void listHasTheFiveDescribedAbilities() {
        assertEquals(5, AtaqueCorpoACorpoCompetencyAbility.values().length);
    }

    @Test
    void noAbilityReducesDifficultyOrGrantsASkillRollBonusYet() {
        ModifierResolver modifierResolver = new ModifierResolverImpl();
        for (AtaqueCorpoACorpoCompetencyAbility ability : AtaqueCorpoACorpoCompetencyAbility.values()) {
            assertEquals(0, ability.getDifficultyReduction());
            assertEquals(0, modifierResolver.sumModifiers(ability, ModifierType.SKILL_ROLL_BONUS));
        }
    }
}
