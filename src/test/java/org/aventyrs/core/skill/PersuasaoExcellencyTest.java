package org.aventyrs.core.skill;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class PersuasaoExcellencyTest {

    @Test
    void everyExcellencyBelongsToPersuasao() {
        for (PersuasaoExcellency excellency : PersuasaoExcellency.values()) {
            assertEquals(SkillType.PERSUASAO, excellency.getSkillType());
        }
    }

    @Test
    void everyExcellencyHasADescription() {
        for (PersuasaoExcellency excellency : PersuasaoExcellency.values()) {
            assertFalse(excellency.getDescription().isBlank());
        }
    }

    @Test
    void listHasTheThreeUniversalTiers() {
        assertEquals(3, PersuasaoExcellency.values().length);
    }

    @Test
    void onlyProdigioReducesDifficulty() {
        for (PersuasaoExcellency excellency : PersuasaoExcellency.values()) {
            int expected = excellency == PersuasaoExcellency.PRODIGIO ? 1 : 0;
            assertEquals(expected, excellency.getDifficultyReduction());
        }
    }

    @Test
    void unlockedByFiltersPersuasaoExcellencyByGraduation() {
        assertEquals(List.of(), SkillExcellency.unlockedBy(PersuasaoExcellency.class, 2));
        assertEquals(List.of(PersuasaoExcellency.FOCADO), SkillExcellency.unlockedBy(PersuasaoExcellency.class, 5));
        assertEquals(List.of(PersuasaoExcellency.FOCADO, PersuasaoExcellency.PRODIGIO, PersuasaoExcellency.LENDA),
                SkillExcellency.unlockedBy(PersuasaoExcellency.class, 10));
    }
}
