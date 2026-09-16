package org.aventyrs.core.skill.esquivaeaparar;

import org.aventyrs.core.item.ItemWeightClass;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.Test;

import java.util.Set;

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

    @Test
    void everySpecializationReportsTheWeightClassesItsDescriptionNames() {
        assertEquals(Set.of(ItemWeightClass.LIGHT), EsquivaEApararSpecialization.LUTADOR_LEVE.getWeightClasses());
        assertEquals(Set.of(ItemWeightClass.MEDIUM),
                EsquivaEApararSpecialization.SOLDADO_DE_INFANTARIA.getWeightClasses());
        assertEquals(Set.of(ItemWeightClass.HEAVY), EsquivaEApararSpecialization.PESO_PESADO.getWeightClasses());
        assertEquals(Set.of(), EsquivaEApararSpecialization.PROTECAO_TECNOLOGICA.getWeightClasses());
        assertEquals(Set.of(), EsquivaEApararSpecialization.GUERREIRO_NATURAL.getWeightClasses());
    }
}
