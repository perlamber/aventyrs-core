package org.aventyrs.core.skill.profissao;

import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ProfissaoSpecializationTest {

    @Test
    void everySpecializationHasADescription() {
        for (ProfissaoSpecialization specialization : ProfissaoSpecialization.values()) {
            assertFalse(specialization.getDescription().isBlank());
        }
    }

    @Test
    void listHasTheFiveDescribedSpecializations() {
        assertEquals(5, ProfissaoSpecialization.values().length);
    }

    @Test
    void everySpecializationReportsTheProfissaoSkillType() {
        for (ProfissaoSpecialization specialization : ProfissaoSpecialization.values()) {
            assertEquals(SkillType.PROFISSAO, specialization.getSkillType());
        }
    }
}
