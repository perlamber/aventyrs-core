package org.aventyrs.core.skill.medicinaecura;

import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class MedicinaECuraSpecializationTest {

    @Test
    void everySpecializationHasADescription() {
        for (MedicinaECuraSpecialization specialization : MedicinaECuraSpecialization.values()) {
            assertFalse(specialization.getDescription().isBlank());
        }
    }

    @Test
    void listHasTheFiveDescribedSpecializations() {
        assertEquals(5, MedicinaECuraSpecialization.values().length);
    }

    @Test
    void everySpecializationReportsTheMedicinaECuraSkillType() {
        for (MedicinaECuraSpecialization specialization : MedicinaECuraSpecialization.values()) {
            assertEquals(SkillType.MEDICINA_E_CURA, specialization.getSkillType());
        }
    }
}
