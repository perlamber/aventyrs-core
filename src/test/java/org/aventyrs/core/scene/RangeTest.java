package org.aventyrs.core.scene;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RangeTest {

    @Test
    void everyRangeIsWithinItself() {
        for (Range range : Range.values()) {
            assertTrue(range.isWithin(range));
        }
    }

    @Test
    void nearerRangesAreWithinFartherOnes() {
        assertTrue(Range.ADJACENTE.isWithin(Range.DISTANCIA_CURTA));
        assertTrue(Range.DISTANCIA_CURTA.isWithin(Range.DISTANCIA_MEDIA));
        assertTrue(Range.DISTANCIA_MEDIA.isWithin(Range.DISTANCIA_LONGA));
        assertTrue(Range.DISTANCIA_LONGA.isWithin(Range.DISTANCIA_MUITO_LONGA));
    }

    @Test
    void fartherRangesAreNotWithinNearerOnes() {
        assertFalse(Range.DISTANCIA_MUITO_LONGA.isWithin(Range.DISTANCIA_LONGA));
        assertFalse(Range.DISTANCIA_CURTA.isWithin(Range.ADJACENTE));
    }
}
