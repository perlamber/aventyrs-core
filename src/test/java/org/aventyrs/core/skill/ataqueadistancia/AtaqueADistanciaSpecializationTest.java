package org.aventyrs.core.skill.ataqueadistancia;

import org.aventyrs.core.item.ItemWeightClass;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AtaqueADistanciaSpecializationTest {

    @Test
    void everySpecializationHasADescription() {
        for (AtaqueADistanciaSpecialization specialization : AtaqueADistanciaSpecialization.values()) {
            assertFalse(specialization.getDescription().isBlank());
        }
    }

    @Test
    void listHasTheFiveDescribedSpecializations() {
        assertEquals(5, AtaqueADistanciaSpecialization.values().length);
    }

    @Test
    void everySpecializationReportsTheAtaqueADistanciaSkillType() {
        for (AtaqueADistanciaSpecialization specialization : AtaqueADistanciaSpecialization.values()) {
            assertEquals(SkillType.ATAQUE_A_DISTANCIA, specialization.getSkillType());
        }
    }

    @Test
    void everySpecializationReportsTheWeightClassesItsDescriptionNames() {
        assertEquals(Set.of(), AtaqueADistanciaSpecialization.TECNICAS_DE_ARREMESSO.getWeightClasses());
        assertEquals(Set.of(), AtaqueADistanciaSpecialization.ARMAS_TECNOLOGICAS.getWeightClasses());
        assertEquals(Set.of(ItemWeightClass.LIGHT),
                AtaqueADistanciaSpecialization.ARTILHARIA_LEVE.getWeightClasses());
        assertEquals(Set.of(ItemWeightClass.MEDIUM, ItemWeightClass.HEAVY),
                AtaqueADistanciaSpecialization.ARTILHARIA_PESADA.getWeightClasses());
        assertEquals(Set.of(), AtaqueADistanciaSpecialization.CONJURADOR_DE_LINHA_DE_TRAS.getWeightClasses());
    }
}
