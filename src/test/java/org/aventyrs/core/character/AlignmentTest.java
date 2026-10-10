package org.aventyrs.core.character;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AlignmentTest {

    @Test
    void everyTendenciaHasItsText() {
        String[] expected = {
                "Maligno: Destruidor", "Maligno: Destruidor",
                "Maligno: Vil", "Maligno: Vil",
                "Neutro", "Neutro",
                "Bondoso: Benevolente", "Bondoso: Benevolente",
                "Bondoso: Samaritano", "Bondoso: Samaritano"
        };
        for (int value = Alignment.MIN; value <= Alignment.MAX; value++) {
            assertEquals(expected[value - 1], Alignment.label(value), "tendência " + value);
        }
    }

    @Test
    void displayIsTheNumberFollowedByItsText() {
        assertEquals("10 - Bondoso: Samaritano", Alignment.display(10));
        assertEquals("6 - Neutro", Alignment.display(Alignment.DEFAULT));
        assertEquals("1 - Maligno: Destruidor", Alignment.display(1));
    }

    @Test
    void bandsSplitAtFourFiveAndSixSeven() {
        assertEquals(Alignment.EVIL, Alignment.of(1));
        assertEquals(Alignment.EVIL, Alignment.of(4));
        assertEquals(Alignment.NEUTRAL, Alignment.of(5));
        assertEquals(Alignment.NEUTRAL, Alignment.of(6));
        assertEquals(Alignment.GOOD, Alignment.of(7));
        assertEquals(Alignment.GOOD, Alignment.of(10));
    }

    @Test
    void outOfRangeTendenciaIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> Alignment.of(0));
        assertThrows(IllegalArgumentException.class, () -> Alignment.label(11));
    }
}
