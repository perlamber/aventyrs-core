package org.aventyrs.core.skill;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AtletismoCompetencyAbilityTest {

    @Test
    void everyAbilityBelongsToAtletismo() {
        for (AtletismoCompetencyAbility ability : AtletismoCompetencyAbility.values()) {
            assertEquals(SkillType.ATLETISMO, ability.getSkillType());
        }
    }

    @Test
    void everyAbilityHasADescription() {
        for (AtletismoCompetencyAbility ability : AtletismoCompetencyAbility.values()) {
            assertFalse(ability.getDescription().isBlank());
        }
    }

    @Test
    void listHasTheFiveDescribedAbilities() {
        assertEquals(5, AtletismoCompetencyAbility.values().length);
    }

    @Test
    void onlyAtletaVersatilReducesDifficulty() {
        for (AtletismoCompetencyAbility ability : AtletismoCompetencyAbility.values()) {
            int expected = ability == AtletismoCompetencyAbility.ATLETA_VERSATIL ? 1 : 0;
            assertEquals(expected, ability.getDifficultyReduction());
        }
    }
}
