package org.aventyrs.core.skill.persuasao;

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
}
