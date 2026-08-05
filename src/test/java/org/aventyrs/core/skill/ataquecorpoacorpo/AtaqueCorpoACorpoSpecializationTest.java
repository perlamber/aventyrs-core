package org.aventyrs.core.skill.ataquecorpoacorpo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AtaqueCorpoACorpoSpecializationTest {

    @Test
    void everySpecializationHasADescription() {
        for (AtaqueCorpoACorpoSpecialization specialization : AtaqueCorpoACorpoSpecialization.values()) {
            assertFalse(specialization.getDescription().isBlank());
        }
    }

    @Test
    void listHasTheFiveDescribedSpecializations() {
        assertEquals(5, AtaqueCorpoACorpoSpecialization.values().length);
    }
}
