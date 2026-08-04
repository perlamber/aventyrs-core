package org.aventyrs.core.scene.grid;

import org.aventyrs.core.scene.Range;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RangeBandTest {

    @Test
    void bandsHexDistanceIntoRange() {
        assertEquals(Range.ADJACENTE, RangeBand.fromHexDistance(0));
        assertEquals(Range.ADJACENTE, RangeBand.fromHexDistance(1));
        assertEquals(Range.DISTANCIA_CURTA, RangeBand.fromHexDistance(2));
        assertEquals(Range.DISTANCIA_MEDIA, RangeBand.fromHexDistance(3));
        assertEquals(Range.DISTANCIA_MEDIA, RangeBand.fromHexDistance(4));
        assertEquals(Range.DISTANCIA_LONGA, RangeBand.fromHexDistance(5));
        assertEquals(Range.DISTANCIA_LONGA, RangeBand.fromHexDistance(8));
        assertEquals(Range.DISTANCIA_MUITO_LONGA, RangeBand.fromHexDistance(9));
        assertEquals(Range.DISTANCIA_MUITO_LONGA, RangeBand.fromHexDistance(50));
    }
}
