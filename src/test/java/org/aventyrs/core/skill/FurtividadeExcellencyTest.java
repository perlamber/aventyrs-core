package org.aventyrs.core.skill;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class FurtividadeExcellencyTest {

    @Test
    void everyExcellencyBelongsToFurtividade() {
        for (FurtividadeExcellency excellency : FurtividadeExcellency.values()) {
            assertEquals(SkillType.FURTIVIDADE, excellency.getSkillType());
        }
    }

    @Test
    void everyExcellencyHasADescription() {
        for (FurtividadeExcellency excellency : FurtividadeExcellency.values()) {
            assertFalse(excellency.getDescription().isBlank());
        }
    }

    @Test
    void listHasTheThreeUniversalTiers() {
        assertEquals(3, FurtividadeExcellency.values().length);
    }

    @Test
    void onlyProdigioReducesDifficulty() {
        for (FurtividadeExcellency excellency : FurtividadeExcellency.values()) {
            int expected = excellency == FurtividadeExcellency.PRODIGIO ? 1 : 0;
            assertEquals(expected, excellency.getDifficultyReduction());
        }
    }

    @Test
    void unlockedByFiltersFurtividadeExcellencyByGraduation() {
        assertEquals(List.of(), SkillExcellency.unlockedBy(FurtividadeExcellency.class, 2));
        assertEquals(List.of(FurtividadeExcellency.FOCADO), SkillExcellency.unlockedBy(FurtividadeExcellency.class, 5));
        assertEquals(List.of(FurtividadeExcellency.FOCADO, FurtividadeExcellency.PRODIGIO, FurtividadeExcellency.LENDA),
                SkillExcellency.unlockedBy(FurtividadeExcellency.class, 10));
    }
}
