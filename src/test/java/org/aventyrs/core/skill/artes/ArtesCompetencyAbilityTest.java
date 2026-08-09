package org.aventyrs.core.skill.artes;

import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ArtesCompetencyAbilityTest {

    @Test
    void everyAbilityBelongsToArtes() {
        for (ArtesCompetencyAbility ability : ArtesCompetencyAbility.values()) {
            assertEquals(SkillType.ARTES, ability.getSkillType());
        }
    }

    @Test
    void everyAbilityHasADescription() {
        for (ArtesCompetencyAbility ability : ArtesCompetencyAbility.values()) {
            assertFalse(ability.getDescription().isBlank());
        }
    }

    @Test
    void listHasTheFiveDescribedAbilities() {
        assertEquals(5, ArtesCompetencyAbility.values().length);
    }
}
