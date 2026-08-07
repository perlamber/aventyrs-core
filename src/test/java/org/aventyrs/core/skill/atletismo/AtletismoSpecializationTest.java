package org.aventyrs.core.skill.atletismo;

import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AtletismoSpecializationTest {

    @Test
    void everySpecializationHasADescription() {
        for (AtletismoSpecialization specialization : AtletismoSpecialization.values()) {
            assertFalse(specialization.getDescription().isBlank());
        }
    }

    @Test
    void listHasTheFiveDescribedSpecializations() {
        assertEquals(5, AtletismoSpecialization.values().length);
    }

    @Test
    void everySpecializationReportsTheAtletismoSkillType() {
        for (AtletismoSpecialization specialization : AtletismoSpecialization.values()) {
            assertEquals(SkillType.ATLETISMO, specialization.getSkillType());
        }
    }
}
