package org.aventyrs.core.skill;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AtaqueADistanciaExcellencyTest {

    @Test
    void everyExcellencyBelongsToAtaqueADistancia() {
        for (AtaqueADistanciaExcellency excellency : AtaqueADistanciaExcellency.values()) {
            assertEquals(SkillType.ATAQUE_A_DISTANCIA, excellency.getSkillType());
        }
    }

    @Test
    void everyExcellencyHasADescription() {
        for (AtaqueADistanciaExcellency excellency : AtaqueADistanciaExcellency.values()) {
            assertFalse(excellency.getDescription().isBlank());
        }
    }

    @Test
    void listHasTheThreeUniversalTiers() {
        assertEquals(3, AtaqueADistanciaExcellency.values().length);
    }

    @Test
    void onlyProdigioReducesDifficulty() {
        for (AtaqueADistanciaExcellency excellency : AtaqueADistanciaExcellency.values()) {
            int expected = excellency == AtaqueADistanciaExcellency.PRODIGIO ? 1 : 0;
            assertEquals(expected, excellency.getDifficultyReduction());
        }
    }

    @Test
    void unlockedByFiltersAtaqueADistanciaExcellencyByGraduation() {
        assertEquals(List.of(), SkillExcellency.unlockedBy(AtaqueADistanciaExcellency.class, 2));
        assertEquals(List.of(AtaqueADistanciaExcellency.FOCADO), SkillExcellency.unlockedBy(AtaqueADistanciaExcellency.class, 5));
        assertEquals(List.of(AtaqueADistanciaExcellency.FOCADO, AtaqueADistanciaExcellency.PRODIGIO, AtaqueADistanciaExcellency.LENDA),
                SkillExcellency.unlockedBy(AtaqueADistanciaExcellency.class, 10));
    }
}
