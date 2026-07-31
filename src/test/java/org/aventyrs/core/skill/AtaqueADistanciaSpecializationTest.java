package org.aventyrs.core.skill;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AtaqueADistanciaSpecializationTest {

    @Test
    void everySpecializationHasADescription() {
        for (AtaqueADistanciaSpecialization specialization : AtaqueADistanciaSpecialization.values()) {
            assertFalse(specialization.getDescription().isBlank());
        }
    }

    @Test
    void listHasTheFiveDescribedSpecializations() {
        assertEquals(5, AtaqueADistanciaSpecialization.values().length);
    }
}
