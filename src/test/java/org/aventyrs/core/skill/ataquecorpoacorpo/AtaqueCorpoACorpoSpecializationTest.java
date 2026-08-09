package org.aventyrs.core.skill.ataquecorpoacorpo;

import org.aventyrs.core.skill.SkillType;
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

    @Test
    void everySpecializationReportsTheAtaqueCorpoACorpoSkillType() {
        for (AtaqueCorpoACorpoSpecialization specialization : AtaqueCorpoACorpoSpecialization.values()) {
            assertEquals(SkillType.ATAQUE_CORPO_A_CORPO, specialization.getSkillType());
        }
    }
}
