package org.aventyrs.core.scene.grid;

import java.util.ArrayList;
import java.util.List;

/**
 * Distance on a flat-top, "even-q" offset hex grid (see {@link GridPosition}). Offset
 * coordinates alias what looks like simple row/column math to a genuinely different adjacency
 * structure — alternating columns are staggered by half a cell, so {@code |Δx| + |Δy|} gives
 * wrong answers for roughly half of all position pairs. This converts through cube coordinates
 * instead, the standard technique for hex-grid distance: every hex maps to a point (x, y, z)
 * with x + y + z = 0, and the distance between two hexes is the largest single-axis delta
 * between their cube coordinates.
 */
public final class HexGrid {

    private HexGrid() {
    }

    /** Number of hex steps between two positions on the grid. */
    public static int distance(GridPosition a, GridPosition b) {
        int[] cubeA = toCube(a);
        int[] cubeB = toCube(b);
        int deltaX = Math.abs(cubeA[0] - cubeB[0]);
        int deltaY = Math.abs(cubeA[1] - cubeB[1]);
        int deltaZ = Math.abs(cubeA[2] - cubeB[2]);
        return Math.max(deltaX, Math.max(deltaY, deltaZ));
    }

    /**
     * The hexes one step from position that lie on a columns by rows board — never constructing
     * an off-board {@link GridPosition}, whose constructor throws. Adjacency is read off {@link
     * #distance} itself (every candidate at distance 1), so the two can never disagree about which
     * way the columns are staggered.
     */
    public static List<GridPosition> neighbours(final GridPosition position, final int columns, final int rows) {
        int maxX = Math.min(columns, GridPosition.GRID_SIZE);
        int maxY = Math.min(rows, GridPosition.GRID_SIZE);
        List<GridPosition> neighbours = new ArrayList<>(6);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                int x = position.x() + dx;
                int y = position.y() + dy;
                if ((dx == 0 && dy == 0) || x < 0 || y < 0 || x >= maxX || y >= maxY) {
                    continue;
                }
                GridPosition candidate = new GridPosition(x, y);
                if (distance(position, candidate) == 1) {
                    neighbours.add(candidate);
                }
            }
        }
        return neighbours;
    }

    private static int[] toCube(GridPosition position) {
        int cubeX = position.x();
        int cubeZ = position.y() - position.x() / 2;
        int cubeY = -cubeX - cubeZ;
        return new int[] {cubeX, cubeY, cubeZ};
    }
}
