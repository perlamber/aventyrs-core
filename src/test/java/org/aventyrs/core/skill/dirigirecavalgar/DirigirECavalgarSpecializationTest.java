package org.aventyrs.core.skill.dirigirecavalgar;

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
}
