package org.aventyrs.core.skill.ataquecorpoacorpo;

import org.aventyrs.core.item.ItemWeightClass;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.Test;

import java.util.Set;

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

    @Test
    void everySpecializationReportsTheWeightClassesItsDescriptionNames() {
        assertEquals(Set.of(ItemWeightClass.LIGHT, ItemWeightClass.MEDIUM),
                AtaqueCorpoACorpoSpecialization.INFANTARIA_LEVE.getWeightClasses());
        assertEquals(Set.of(ItemWeightClass.HEAVY),
                AtaqueCorpoACorpoSpecialization.INFANTARIA_PESADA.getWeightClasses());
        assertEquals(Set.of(), AtaqueCorpoACorpoSpecialization.ARCANISTA_DE_LINHA_DE_FRENTE.getWeightClasses());
        assertEquals(Set.of(), AtaqueCorpoACorpoSpecialization.PRIMAL.getWeightClasses());
        assertEquals(Set.of(), AtaqueCorpoACorpoSpecialization.ARMAS_TECNOLOGICAS.getWeightClasses());
    }
}
