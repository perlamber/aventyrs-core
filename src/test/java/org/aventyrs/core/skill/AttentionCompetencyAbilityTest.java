package org.aventyrs.core.skill;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AttentionCompetencyAbilityTest {

    @Test
    void everyAbilityBelongsToAttention() {
        for (AttentionCompetencyAbility ability : AttentionCompetencyAbility.values()) {
            assertEquals(SkillType.ATTENTION, ability.getSkillType());
        }
    }

    @Test
    void everyAbilityHasADescription() {
        for (AttentionCompetencyAbility ability : AttentionCompetencyAbility.values()) {
            assertFalse(ability.getDescription().isBlank());
        }
    }

    @Test
    void listHasTheFiveDescribedAbilities() {
        assertEquals(5, AttentionCompetencyAbility.values().length);
    }
}
