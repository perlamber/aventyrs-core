package org.aventyrs.core.skill.dominiodomana;

import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class DominioDoManaSpecializationTest {

    @Test
    void everySpecializationHasADescription() {
        for (DominioDoManaSpecialization specialization : DominioDoManaSpecialization.values()) {
            assertFalse(specialization.getDescription().isBlank());
        }
    }

    @Test
    void listHasTheFiveDescribedSpecializations() {
        assertEquals(5, DominioDoManaSpecialization.values().length);
    }

    @Test
    void everySpecializationReportsTheDominioDoManaSkillType() {
        for (DominioDoManaSpecialization specialization : DominioDoManaSpecialization.values()) {
            assertEquals(SkillType.DOMINIO_DO_MANA, specialization.getSkillType());
        }
    }
}
