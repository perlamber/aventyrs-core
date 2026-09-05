package org.aventyrs.core.scene;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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
        assertTrue(Range.ADJACENTE.isWithin(Range.DISTANCIA_MUITO_CURTA));
        assertTrue(Range.DISTANCIA_MUITO_CURTA.isWithin(Range.DISTANCIA_CURTA));
        assertTrue(Range.DISTANCIA_CURTA.isWithin(Range.DISTANCIA_MEDIA));
        assertTrue(Range.DISTANCIA_MEDIA.isWithin(Range.DISTANCIA_LONGA));
        assertTrue(Range.DISTANCIA_LONGA.isWithin(Range.DISTANCIA_MUITO_LONGA));
        assertTrue(Range.DISTANCIA_MUITO_LONGA.isWithin(Range.AO_ALCANCE_DOS_OLHOS));
    }

    @Test
    void fartherRangesAreNotWithinNearerOnes() {
        assertFalse(Range.AO_ALCANCE_DOS_OLHOS.isWithin(Range.DISTANCIA_MUITO_LONGA));
        assertFalse(Range.DISTANCIA_MUITO_LONGA.isWithin(Range.DISTANCIA_LONGA));
        assertFalse(Range.DISTANCIA_MUITO_CURTA.isWithin(Range.ADJACENTE));
        assertFalse(Range.DISTANCIA_CURTA.isWithin(Range.ADJACENTE));
    }

    @Test
    void everyRangeExceptAoAlcanceDosOlhosHasAFixedMaxUnidadesDeDistancia() {
        assertEquals(1, Range.ADJACENTE.getMaxUnidadesDeDistancia());
        assertEquals(2, Range.DISTANCIA_MUITO_CURTA.getMaxUnidadesDeDistancia());
        assertEquals(4, Range.DISTANCIA_CURTA.getMaxUnidadesDeDistancia());
        assertEquals(8, Range.DISTANCIA_MEDIA.getMaxUnidadesDeDistancia());
        assertEquals(16, Range.DISTANCIA_LONGA.getMaxUnidadesDeDistancia());
        assertEquals(24, Range.DISTANCIA_MUITO_LONGA.getMaxUnidadesDeDistancia());
        assertNull(Range.AO_ALCANCE_DOS_OLHOS.getMaxUnidadesDeDistancia());
    }

    @Test
    void fromUnidadesDeDistanciaResolvesTheNearestCoveringBand() {
        assertEquals(Range.ADJACENTE, Range.fromUnidadesDeDistancia(0));
        assertEquals(Range.ADJACENTE, Range.fromUnidadesDeDistancia(1));
        assertEquals(Range.DISTANCIA_MUITO_CURTA, Range.fromUnidadesDeDistancia(2));
        assertEquals(Range.DISTANCIA_CURTA, Range.fromUnidadesDeDistancia(3));
        assertEquals(Range.DISTANCIA_CURTA, Range.fromUnidadesDeDistancia(4));
        assertEquals(Range.DISTANCIA_MEDIA, Range.fromUnidadesDeDistancia(8));
        assertEquals(Range.DISTANCIA_LONGA, Range.fromUnidadesDeDistancia(16));
        assertEquals(Range.DISTANCIA_MUITO_LONGA, Range.fromUnidadesDeDistancia(24));
    }

    @Test
    void fromUnidadesDeDistanciaResolvesBeyondTwentyFourAsAoAlcanceDosOlhos() {
        assertEquals(Range.AO_ALCANCE_DOS_OLHOS, Range.fromUnidadesDeDistancia(25));
        assertEquals(Range.AO_ALCANCE_DOS_OLHOS, Range.fromUnidadesDeDistancia(1000));
    }

    @Test
    void increasedByShiftsUpTheBandLadder() {
        assertEquals(Range.DISTANCIA_MEDIA, Range.DISTANCIA_CURTA.increasedBy(1));
        assertEquals(Range.DISTANCIA_MUITO_LONGA, Range.DISTANCIA_MEDIA.increasedBy(2));
        assertEquals(Range.DISTANCIA_MUITO_CURTA, Range.ADJACENTE.increasedBy(1));
    }

    @Test
    void increasedByZeroIsIdentity() {
        for (Range range : Range.values()) {
            assertEquals(range, range.increasedBy(0));
        }
    }

    @Test
    void increasedByClampsAtAoAlcanceDosOlhos() {
        assertEquals(Range.AO_ALCANCE_DOS_OLHOS, Range.DISTANCIA_MUITO_LONGA.increasedBy(1));
        assertEquals(Range.AO_ALCANCE_DOS_OLHOS, Range.DISTANCIA_CURTA.increasedBy(99));
        assertEquals(Range.AO_ALCANCE_DOS_OLHOS, Range.AO_ALCANCE_DOS_OLHOS.increasedBy(3));
    }

    @Test
    void increasedByClampsAtAdjacenteForNegativeSteps() {
        assertEquals(Range.ADJACENTE, Range.DISTANCIA_CURTA.increasedBy(-5));
        assertEquals(Range.DISTANCIA_MUITO_CURTA, Range.DISTANCIA_MEDIA.increasedBy(-2));
    }
}
