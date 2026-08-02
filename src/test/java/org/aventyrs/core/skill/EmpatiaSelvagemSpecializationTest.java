package org.aventyrs.core.skill;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class EmpatiaSelvagemSpecializationTest {

    @Test
    void everySpecializationHasADescription() {
        for (EmpatiaSelvagemSpecialization specialization : EmpatiaSelvagemSpecialization.values()) {
            assertFalse(specialization.getDescription().isBlank());
        }
    }

    @Test
    void listHasTheFiveDescribedSpecializations() {
        assertEquals(5, EmpatiaSelvagemSpecialization.values().length);
    }
}
