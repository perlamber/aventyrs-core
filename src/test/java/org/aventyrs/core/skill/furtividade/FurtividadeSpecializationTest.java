package org.aventyrs.core.skill.furtividade;

import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class FurtividadeSpecializationTest {

    @Test
    void everySpecializationHasADescription() {
        for (FurtividadeSpecialization specialization : FurtividadeSpecialization.values()) {
            assertFalse(specialization.getDescription().isBlank());
        }
    }

    @Test
    void listHasTheFiveDescribedSpecializations() {
        assertEquals(5, FurtividadeSpecialization.values().length);
    }

    @Test
    void everySpecializationReportsTheFurtividadeSkillType() {
        for (FurtividadeSpecialization specialization : FurtividadeSpecialization.values()) {
            assertEquals(SkillType.FURTIVIDADE, specialization.getSkillType());
        }
    }
}
