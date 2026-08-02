package org.aventyrs.core.skill;

import org.aventyrs.core.modifier.ModifierResolver;
import org.aventyrs.core.modifier.ModifierResolverImpl;
import org.aventyrs.core.modifier.ModifierType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class DominioDoManaCompetencyAbilityTest {

    @Test
    void everyAbilityBelongsToDominioDoMana() {
        for (DominioDoManaCompetencyAbility ability : DominioDoManaCompetencyAbility.values()) {
            assertEquals(SkillType.DOMINIO_DO_MANA, ability.getSkillType());
        }
    }

    @Test
    void everyAbilityHasADescription() {
        for (DominioDoManaCompetencyAbility ability : DominioDoManaCompetencyAbility.values()) {
            assertFalse(ability.getDescription().isBlank());
        }
    }

    @Test
    void listHasTheFiveDescribedAbilities() {
        assertEquals(5, DominioDoManaCompetencyAbility.values().length);
    }

    @Test
    void noAbilityReducesDifficultyOrGrantsASkillRollBonusYet() {
        ModifierResolver modifierResolver = new ModifierResolverImpl();
        for (DominioDoManaCompetencyAbility ability : DominioDoManaCompetencyAbility.values()) {
            assertEquals(0, ability.getDifficultyReduction());
            assertEquals(0, modifierResolver.sumModifiers(ability, ModifierType.SKILL_ROLL_BONUS));
        }
    }
}
