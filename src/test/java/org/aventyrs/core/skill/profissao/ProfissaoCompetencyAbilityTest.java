package org.aventyrs.core.skill.profissao;

import org.aventyrs.core.modifier.ModifierResolver;
import org.aventyrs.core.modifier.ModifierResolverImpl;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ProfissaoCompetencyAbilityTest {

    @Test
    void everyAbilityBelongsToProfissao() {
        for (ProfissaoCompetencyAbility ability : ProfissaoCompetencyAbility.values()) {
            assertEquals(SkillType.PROFISSAO, ability.getSkillType());
        }
    }

    @Test
    void everyAbilityHasADescription() {
        for (ProfissaoCompetencyAbility ability : ProfissaoCompetencyAbility.values()) {
            assertFalse(ability.getDescription().isBlank());
        }
    }

    @Test
    void listHasTheFiveDescribedAbilities() {
        assertEquals(5, ProfissaoCompetencyAbility.values().length);
    }

    @Test
    void noAbilityReducesDifficultyOrGrantsASkillRollBonusYet() {
        ModifierResolver modifierResolver = new ModifierResolverImpl();
        for (ProfissaoCompetencyAbility ability : ProfissaoCompetencyAbility.values()) {
            assertEquals(0, ability.getDifficultyReduction());
            assertEquals(0, modifierResolver.sumModifiers(ability, ModifierType.SKILL_ROLL_BONUS));
        }
    }
}
