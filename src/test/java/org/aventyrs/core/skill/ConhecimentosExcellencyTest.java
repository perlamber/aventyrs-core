package org.aventyrs.core.skill;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ConhecimentosExcellencyTest {

    @Test
    void everyExcellencyBelongsToConhecimentos() {
        for (ConhecimentosExcellency excellency : ConhecimentosExcellency.values()) {
            assertEquals(SkillType.CONHECIMENTOS, excellency.getSkillType());
        }
    }

    @Test
    void everyExcellencyHasADescription() {
        for (ConhecimentosExcellency excellency : ConhecimentosExcellency.values()) {
            assertFalse(excellency.getDescription().isBlank());
        }
    }

    @Test
    void listHasTheThreeUniversalTiers() {
        assertEquals(3, ConhecimentosExcellency.values().length);
    }

    @Test
    void onlyProdigioReducesDifficulty() {
        for (ConhecimentosExcellency excellency : ConhecimentosExcellency.values()) {
            int expected = excellency == ConhecimentosExcellency.PRODIGIO ? 1 : 0;
            assertEquals(expected, excellency.getDifficultyReduction());
        }
    }

    @Test
    void unlockedByFiltersConhecimentosExcellencyByGraduation() {
        assertEquals(List.of(), SkillExcellency.unlockedBy(ConhecimentosExcellency.class, 2));
        assertEquals(List.of(ConhecimentosExcellency.FOCADO), SkillExcellency.unlockedBy(ConhecimentosExcellency.class, 5));
        assertEquals(List.of(ConhecimentosExcellency.FOCADO, ConhecimentosExcellency.PRODIGIO, ConhecimentosExcellency.LENDA),
                SkillExcellency.unlockedBy(ConhecimentosExcellency.class, 10));
    }
}
