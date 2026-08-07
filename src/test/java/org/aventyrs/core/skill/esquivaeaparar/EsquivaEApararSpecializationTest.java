package org.aventyrs.core.skill.esquivaeaparar;

import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class EsquivaEApararSpecializationTest {

    @Test
    void everySpecializationHasADescription() {
        for (EsquivaEApararSpecialization specialization : EsquivaEApararSpecialization.values()) {
            assertFalse(specialization.getDescription().isBlank());
        }
    }

    @Test
    void listHasTheFiveDescribedSpecializations() {
        assertEquals(5, EsquivaEApararSpecialization.values().length);
    }

    @Test
    void everySpecializationReportsTheEsquivaEApararSkillType() {
        for (EsquivaEApararSpecialization specialization : EsquivaEApararSpecialization.values()) {
            assertEquals(SkillType.ESQUIVA_E_APARAR, specialization.getSkillType());
        }
    }
}
