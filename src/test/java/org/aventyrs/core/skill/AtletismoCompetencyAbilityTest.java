package org.aventyrs.core.skill;

import org.aventyrs.core.character.AttributeDomain;
import org.junit.jupiter.api.Test;

import java.util.Optional;

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
    void noAbilityReducesDifficulty() {
        for (AtletismoCompetencyAbility ability : AtletismoCompetencyAbility.values()) {
            assertEquals(0, ability.getDifficultyReduction());
        }
    }

    @Test
    void onlyAcrobataSubstitutesTheBaseAttribute() {
        for (AtletismoCompetencyAbility ability : AtletismoCompetencyAbility.values()) {
            if (ability == AtletismoCompetencyAbility.ACROBATA) {
                assertEquals(Optional.of(AttributeDomain.DEXTERITY), ability.getSubstituteAttributeDomain());
            } else {
                assertEquals(Optional.empty(), ability.getSubstituteAttributeDomain());
            }
        }
    }
}
