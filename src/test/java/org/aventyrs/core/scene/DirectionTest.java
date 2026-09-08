package org.aventyrs.core.scene;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DirectionTest {

    @Test
    void oppositePairsTheCardinalDirections() {
        assertEquals(Direction.SOUTH, Direction.NORTH.opposite());
        assertEquals(Direction.NORTH, Direction.SOUTH.opposite());
        assertEquals(Direction.WEST, Direction.EAST.opposite());
        assertEquals(Direction.EAST, Direction.WEST.opposite());
    }

    @Test
    void oppositeIsItsOwnInverse() {
        for (Direction direction : Direction.values()) {
            assertEquals(direction, direction.opposite().opposite());
        }
    }
}
