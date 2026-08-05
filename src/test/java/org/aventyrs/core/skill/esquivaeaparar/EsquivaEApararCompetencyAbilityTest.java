package org.aventyrs.core.skill.esquivaeaparar;

import org.aventyrs.core.modifier.ModifierResolver;
import org.aventyrs.core.modifier.ModifierResolverImpl;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class EsquivaEApararCompetencyAbilityTest {

    @Test
    void everyAbilityBelongsToEsquivaEAparar() {
        for (EsquivaEApararCompetencyAbility ability : EsquivaEApararCompetencyAbility.values()) {
            assertEquals(SkillType.ESQUIVA_E_APARAR, ability.getSkillType());
        }
    }

    @Test
    void everyAbilityHasADescription() {
        for (EsquivaEApararCompetencyAbility ability : EsquivaEApararCompetencyAbility.values()) {
            assertFalse(ability.getDescription().isBlank());
        }
    }

    @Test
    void listHasTheFiveDescribedAbilities() {
        assertEquals(5, EsquivaEApararCompetencyAbility.values().length);
    }

    @Test
    void noAbilityReducesDifficulty() {
        for (EsquivaEApararCompetencyAbility ability : EsquivaEApararCompetencyAbility.values()) {
            assertEquals(0, ability.getDifficultyReduction());
        }
    }

    @Test
    void onlyMovimentoDefensivoGrantsASkillRollBonus() {
        ModifierResolver modifierResolver = new ModifierResolverImpl();
        for (EsquivaEApararCompetencyAbility ability : EsquivaEApararCompetencyAbility.values()) {
            int expected = ability == EsquivaEApararCompetencyAbility.MOVIMENTO_DEFENSIVO ? 3 : 0;
            assertEquals(expected, modifierResolver.sumModifiers(ability, ModifierType.SKILL_ROLL_BONUS));
        }
    }
}
