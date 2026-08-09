package org.aventyrs.core.skill.dirigirecavalgar;

import org.aventyrs.core.skill.SkillExcellency;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class DirigirECavalgarExcellencyTest {

    @Test
    void everyExcellencyBelongsToDirigirECavalgar() {
        for (DirigirECavalgarExcellency excellency : DirigirECavalgarExcellency.values()) {
            assertEquals(SkillType.DIRIGIR_E_CAVALGAR, excellency.getSkillType());
        }
    }

    @Test
    void everyExcellencyHasADescription() {
        for (DirigirECavalgarExcellency excellency : DirigirECavalgarExcellency.values()) {
            assertFalse(excellency.getDescription().isBlank());
        }
    }

    @Test
    void listHasTheThreeUniversalTiers() {
        assertEquals(3, DirigirECavalgarExcellency.values().length);
    }

    @Test
    void onlyProdigioReducesDifficulty() {
        for (DirigirECavalgarExcellency excellency : DirigirECavalgarExcellency.values()) {
            int expected = excellency == DirigirECavalgarExcellency.PRODIGIO ? 1 : 0;
            assertEquals(expected, excellency.getDifficultyReduction());
        }
    }

    @Test
    void unlockedByFiltersDirigirECavalgarExcellencyByGraduation() {
        assertEquals(List.of(), SkillExcellency.unlockedBy(DirigirECavalgarExcellency.class, 2));
        assertEquals(List.of(DirigirECavalgarExcellency.FOCADO), SkillExcellency.unlockedBy(DirigirECavalgarExcellency.class, 5));
        assertEquals(List.of(DirigirECavalgarExcellency.FOCADO, DirigirECavalgarExcellency.PRODIGIO, DirigirECavalgarExcellency.LENDA),
                SkillExcellency.unlockedBy(DirigirECavalgarExcellency.class, 10));
    }
}
