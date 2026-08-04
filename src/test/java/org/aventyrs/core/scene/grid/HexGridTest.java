package org.aventyrs.core.scene.grid;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HexGridTest {

    @Test
    void distanceToSelfIsZero() {
        assertEquals(0, HexGrid.distance(new GridPosition(5, 5), new GridPosition(5, 5)));
    }

    @Test
    void evenColumnNeighborsAreAllDistanceOne() {
        // Column 2 is even: its six neighbors are the two vertical ones plus two each in
        // columns 1 and 3, both one row "above" (relative to column 2's own baseline).
        GridPosition origin = new GridPosition(2, 5);
        GridPosition[] neighbors = {
                new GridPosition(2, 4), new GridPosition(2, 6),
                new GridPosition(1, 4), new GridPosition(1, 5),
                new GridPosition(3, 4), new GridPosition(3, 5)
        };
        for (GridPosition neighbor : neighbors) {
            assertEquals(1, HexGrid.distance(origin, neighbor),
                    () -> "distance from " + origin + " to " + neighbor);
        }
    }

    @Test
    void oddColumnNeighborsAreAllDistanceOne() {
        // Column 3 is odd (shifted down half a cell): its diagonal neighbors in columns 2/4
        // sit one row "below" relative to column 3's own baseline — the mirror image of the
        // even-column case.
        GridPosition origin = new GridPosition(3, 5);
        GridPosition[] neighbors = {
                new GridPosition(3, 4), new GridPosition(3, 6),
                new GridPosition(2, 5), new GridPosition(2, 6),
                new GridPosition(4, 5), new GridPosition(4, 6)
        };
        for (GridPosition neighbor : neighbors) {
            assertEquals(1, HexGrid.distance(origin, neighbor),
                    () -> "distance from " + origin + " to " + neighbor);
        }
    }

    @Test
    void nonNeighborsAreFartherThanOne() {
        GridPosition origin = new GridPosition(2, 5);
        assertEquals(2, HexGrid.distance(origin, new GridPosition(4, 5)));
        assertEquals(2, HexGrid.distance(origin, new GridPosition(2, 7)));
    }

    @Test
    void distanceIsSymmetric() {
        GridPosition a = new GridPosition(10, 20);
        GridPosition b = new GridPosition(30, 15);
        assertEquals(HexGrid.distance(a, b), HexGrid.distance(b, a));
    }

    @Test
    void rejectsOutOfBoundsPositions() {
        assertThrows(IllegalArgumentException.class, () -> new GridPosition(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> new GridPosition(0, GridPosition.GRID_SIZE));
    }
}
