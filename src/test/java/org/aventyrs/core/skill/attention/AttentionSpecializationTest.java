package org.aventyrs.core.skill.attention;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AttentionSpecializationTest {

    @Test
    void everySpecializationHasADescription() {
        for (AttentionSpecialization specialization : AttentionSpecialization.values()) {
            assertFalse(specialization.getDescription().isBlank());
        }
    }

    @Test
    void listHasTheFiveDescribedSpecializations() {
        assertEquals(5, AttentionSpecialization.values().length);
    }
}
