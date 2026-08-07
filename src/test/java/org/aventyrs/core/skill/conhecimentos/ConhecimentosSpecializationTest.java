package org.aventyrs.core.skill.conhecimentos;

import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ConhecimentosSpecializationTest {

    @Test
    void everySpecializationHasADescription() {
        for (ConhecimentosSpecialization specialization : ConhecimentosSpecialization.values()) {
            assertFalse(specialization.getDescription().isBlank());
        }
    }

    @Test
    void listHasTheFiveDescribedSpecializations() {
        assertEquals(5, ConhecimentosSpecialization.values().length);
    }

    @Test
    void everySpecializationReportsTheConhecimentosSkillType() {
        for (ConhecimentosSpecialization specialization : ConhecimentosSpecialization.values()) {
            assertEquals(SkillType.CONHECIMENTOS, specialization.getSkillType());
        }
    }
}
