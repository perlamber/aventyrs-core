package org.aventyrs.core.skill.artes;

import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ArtesSpecializationTest {

    @Test
    void everySpecializationHasADescription() {
        for (ArtesSpecialization specialization : ArtesSpecialization.values()) {
            assertFalse(specialization.getDescription().isBlank());
        }
    }

    @Test
    void listHasTheFiveDescribedSpecializations() {
        assertEquals(5, ArtesSpecialization.values().length);
    }

    @Test
    void everySpecializationReportsTheArtesSkillType() {
        for (ArtesSpecialization specialization : ArtesSpecialization.values()) {
            assertEquals(SkillType.ARTES, specialization.getSkillType());
        }
    }
}
