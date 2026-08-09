package org.aventyrs.core.skill.persuasao;

import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class PersuasaoSpecializationTest {

    @Test
    void everySpecializationHasADescription() {
        for (PersuasaoSpecialization specialization : PersuasaoSpecialization.values()) {
            assertFalse(specialization.getDescription().isBlank());
        }
    }

    @Test
    void listHasTheFiveDescribedSpecializations() {
        assertEquals(5, PersuasaoSpecialization.values().length);
    }

    @Test
    void everySpecializationReportsThePersuasaoSkillType() {
        for (PersuasaoSpecialization specialization : PersuasaoSpecialization.values()) {
            assertEquals(SkillType.PERSUASAO, specialization.getSkillType());
        }
    }
}
