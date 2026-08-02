package org.aventyrs.core.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DiceTest {

    @Test
    void rollDicesStaysWithinTheExpectedRangeForOneDie() {
        int result = Dice.rollDices(1);
        assertTrue(result >= 1 && result <= 6);
    }

    @Test
    void rollDicesStaysWithinTheExpectedRangeForFiveDice() {
        int result = Dice.rollDices(5);
        assertTrue(result >= 5 && result <= 30);
    }

    @Test
    void rollThreeDicesStaysWithinTheExpectedRange() {
        int result = Dice.rollThreeDices();
        assertTrue(result >= 3 && result <= 18);
    }
}
