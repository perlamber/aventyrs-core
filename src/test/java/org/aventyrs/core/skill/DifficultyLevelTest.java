package org.aventyrs.core.skill;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void reachedByReturnsEmptyBelowVeryEasy() {
        assertEquals(Optional.empty(), DifficultyLevel.reachedBy(11));
    }

    @Test
    void reachedByReturnsTheExactTierAtItsOwnBaseValue() {
        assertEquals(Optional.of(DifficultyLevel.VERY_EASY), DifficultyLevel.reachedBy(12));
        assertEquals(Optional.of(DifficultyLevel.MEDIUM), DifficultyLevel.reachedBy(18));
        assertEquals(Optional.of(DifficultyLevel.MIRACLE), DifficultyLevel.reachedBy(60));
    }

    @Test
    void reachedByReturnsTheHighestTierNotExceededByTotal() {
        // 20 clears MEDIUM's 18 but not HARD's 23.
        assertEquals(Optional.of(DifficultyLevel.MEDIUM), DifficultyLevel.reachedBy(20));
    }

    @Test
    void reachedByReturnsTheHardestTierForAnArbitrarilyHighTotal() {
        assertEquals(Optional.of(DifficultyLevel.MIRACLE), DifficultyLevel.reachedBy(1000));
    }

    @Test
    void reachedByIsMonotonicWithIncreasingTotals() {
        Optional<DifficultyLevel> lower = DifficultyLevel.reachedBy(14);
        Optional<DifficultyLevel> higher = DifficultyLevel.reachedBy(28);
        assertTrue(higher.get().ordinal() > lower.get().ordinal());
    }

    @Test
    void reachedByAsExpertReturnsEmptyBelowVeryEasysExpertValue() {
        assertEquals(Optional.empty(), DifficultyLevel.reachedByAsExpert(10));
    }

    @Test
    void reachedByAsExpertReturnsTheExactTierAtItsOwnExpertValue() {
        assertEquals(Optional.of(DifficultyLevel.VERY_EASY), DifficultyLevel.reachedByAsExpert(11));
        assertEquals(Optional.of(DifficultyLevel.MEDIUM), DifficultyLevel.reachedByAsExpert(16));
        assertEquals(Optional.of(DifficultyLevel.MIRACLE), DifficultyLevel.reachedByAsExpert(49));
    }

    @Test
    void reachedByAsExpertReturnsTheHardestTierForAnArbitrarilyHighTotal() {
        assertEquals(Optional.of(DifficultyLevel.MIRACLE), DifficultyLevel.reachedByAsExpert(1000));
    }

    /**
     * The same total (17) clears MEDIUM's easier expertValue (16) but not its baseValue (18) —
     * this is the whole point of a matching Especialização: the same roll reaches a higher tier
     * as an expert than it would as a plain roll.
     */
    @Test
    void reachedByAsExpertReachesAHigherTierThanReachedByForTheSameTotal() {
        assertEquals(Optional.of(DifficultyLevel.EASY), DifficultyLevel.reachedBy(17));
        assertEquals(Optional.of(DifficultyLevel.MEDIUM), DifficultyLevel.reachedByAsExpert(17));
    }
}
