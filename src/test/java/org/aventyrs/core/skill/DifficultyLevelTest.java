package org.aventyrs.core.skill;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DifficultyLevelTest {

    @Test
    void everyLevelHasABaseAndExpertValue() {
        assertEquals(12, DifficultyLevel.VERY_EASY.getBaseValue());
        assertEquals(11, DifficultyLevel.VERY_EASY.getExpertValue());
        assertEquals(60, DifficultyLevel.MIRACLE.getBaseValue());
        assertEquals(49, DifficultyLevel.MIRACLE.getExpertValue());
    }

    @Test
    void harderShiftsToADifficultLevelHigher() {
        assertEquals(DifficultyLevel.HARD, DifficultyLevel.MEDIUM.harder(1));
        assertEquals(DifficultyLevel.VERY_HARD, DifficultyLevel.MEDIUM.harder(2));
    }

    @Test
    void easierShiftsToADifficultLevelLower() {
        assertEquals(DifficultyLevel.EASY, DifficultyLevel.MEDIUM.easier(1));
        assertEquals(DifficultyLevel.VERY_EASY, DifficultyLevel.MEDIUM.easier(2));
    }

    @Test
    void harderClampsAtTheHardestLevel() {
        assertEquals(DifficultyLevel.MIRACLE, DifficultyLevel.MIRACLE.harder(1));
        assertEquals(DifficultyLevel.MIRACLE, DifficultyLevel.VERY_HARD.harder(10));
    }

    @Test
    void easierClampsAtTheEasiestLevel() {
        assertEquals(DifficultyLevel.VERY_EASY, DifficultyLevel.VERY_EASY.easier(1));
        assertEquals(DifficultyLevel.VERY_EASY, DifficultyLevel.HARD.easier(10));
    }
}
