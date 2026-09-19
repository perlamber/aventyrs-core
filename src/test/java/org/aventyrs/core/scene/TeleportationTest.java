package org.aventyrs.core.scene;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeleportationTest {

    @Test
    void aReachIsCarriedInUnidadesDeDistancia() {
        assertEquals(4, new Teleportation(4).unidadesDeDistancia());
    }

    @Test
    void aReachBelowOneUdIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Teleportation(0));
        assertThrows(IllegalArgumentException.class, () -> new Teleportation(-1));
    }

    @Test
    void ofARangeTakesThatBandsOwnMaximum() {
        assertEquals(new Teleportation(4), Teleportation.of(Range.DISTANCIA_CURTA));
        assertEquals(new Teleportation(1), Teleportation.of(Range.ADJACENTE));
        assertEquals(new Teleportation(24), Teleportation.of(Range.DISTANCIA_MUITO_LONGA));
    }

    // AO_ALCANCE_DOS_OLHOS states no maximum UD at all, so there is no number to teleport by.
    @Test
    void ofAnUnboundedRangeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> Teleportation.of(Range.AO_ALCANCE_DOS_OLHOS));
    }

    @Test
    void asRangeIsTheBandCoveringTheReach() {
        assertEquals(Range.DISTANCIA_CURTA, new Teleportation(4).asRange());
        assertEquals(Range.ADJACENTE, new Teleportation(1).asRange());
        // Not a band boundary: 3UD still falls inside Distância Curta's 4.
        assertEquals(Range.DISTANCIA_CURTA, new Teleportation(3).asRange());
    }

    @Test
    void aRangeRoundTripsThroughOfAndAsRange() {
        for (Range range : Range.values()) {
            if (range.getMaxUnidadesDeDistancia() == null) {
                continue;
            }
            assertEquals(range, Teleportation.of(range).asRange());
        }
    }

    @Test
    void aDistanceWithinTheReachIsReachable() {
        Teleportation distanciaCurta = Teleportation.of(Range.DISTANCIA_CURTA);

        assertTrue(distanciaCurta.reaches(Range.ADJACENTE));
        assertTrue(distanciaCurta.reaches(Range.DISTANCIA_MUITO_CURTA));
        assertTrue(distanciaCurta.reaches(Range.DISTANCIA_CURTA));
    }

    @Test
    void aDistanceBeyondTheReachIsNotReachable() {
        Teleportation distanciaCurta = Teleportation.of(Range.DISTANCIA_CURTA);

        assertFalse(distanciaCurta.reaches(Range.DISTANCIA_MEDIA));
        assertFalse(distanciaCurta.reaches(Range.DISTANCIA_LONGA));
        assertFalse(distanciaCurta.reaches(Range.AO_ALCANCE_DOS_OLHOS));
    }

    // "Cannot tell" refuses, the same way a null SceneContext yields nobody elsewhere — a
    // teleport must never be offered to a destination whose distance is unknown.
    @Test
    void anUnknownDistanceIsNotReachable() {
        assertFalse(Teleportation.of(Range.DISTANCIA_CURTA).reaches(null));
    }
}
