package org.aventyrs.core.sheet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TemporaryPointPoolTest {

    private final TemporaryPointPool pool = new TemporaryPointPool();

    @Test
    void startsEmpty() {
        assertEquals(0, pool.getAmount());
    }

    @Test
    void gainAccumulates() {
        pool.gain(2);
        assertEquals(5, pool.gain(3));
    }

    @Test
    void spendReducesTheHeldAmount() {
        pool.gain(5);
        assertEquals(2, pool.spend(3));
    }

    @Test
    void spendNeverGoesBelowZero() {
        pool.gain(2);
        assertEquals(0, pool.spend(10));
    }

    private static final String SOURCE_A = "source-a";
    private static final String SOURCE_B = "source-b";

    @Test
    void gainNonCumulativeRaisesAnEmptyPoolToTheGivenAmount() {
        assertEquals(1, pool.gainNonCumulative(SOURCE_A, 1));
    }

    @Test
    void gainNonCumulativeDoesNotStackOnTopOfAnAlreadyHeldPointFromTheSameSource() {
        pool.gainNonCumulative(SOURCE_A, 1);
        assertEquals(1, pool.gainNonCumulative(SOURCE_A, 1));
    }

    @Test
    void gainNonCumulativeFromADifferentSourceStacksOnTopOfAnotherSourcesContribution() {
        pool.gainNonCumulative(SOURCE_A, 1);
        assertEquals(2, pool.gainNonCumulative(SOURCE_B, 1));
    }

    @Test
    void gainNonCumulativeDoesNotReduceThatSourcesOwnAlreadyHigherContribution() {
        pool.gainNonCumulative(SOURCE_A, 2);
        assertEquals(2, pool.gainNonCumulative(SOURCE_A, 1));
    }

    @Test
    void gainNonCumulativeStacksOnTopOfAnUnrelatedPlainGain() {
        pool.gain(2);
        assertEquals(3, pool.gainNonCumulative(SOURCE_A, 1));
    }
}
