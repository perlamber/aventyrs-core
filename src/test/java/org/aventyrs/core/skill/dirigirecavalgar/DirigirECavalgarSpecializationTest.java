package org.aventyrs.core.skill.dirigirecavalgar;

import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class DirigirECavalgarSpecializationTest {

    @Test
    void everySpecializationHasADescription() {
        for (DirigirECavalgarSpecialization specialization : DirigirECavalgarSpecialization.values()) {
            assertFalse(specialization.getDescription().isBlank());
        }
    }

    @Test
    void listHasTheFiveDescribedSpecializations() {
        assertEquals(5, DirigirECavalgarSpecialization.values().length);
    }

    @Test
    void everySpecializationReportsTheDirigirECavalgarSkillType() {
        for (DirigirECavalgarSpecialization specialization : DirigirECavalgarSpecialization.values()) {
            assertEquals(SkillType.DIRIGIR_E_CAVALGAR, specialization.getSkillType());
        }
    }
}
