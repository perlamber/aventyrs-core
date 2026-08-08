package org.aventyrs.core.scene;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TerrainTypeTest {

    @Test
    void hasTheSixDescribedTerrainTypes() {
        assertEquals(6, TerrainType.values().length);
    }
}
