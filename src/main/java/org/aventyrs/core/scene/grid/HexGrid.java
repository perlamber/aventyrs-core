package org.aventyrs.core.scene.grid;

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

    private static int[] toCube(GridPosition position) {
        int cubeX = position.x();
        int cubeZ = position.y() - position.x() / 2;
        int cubeY = -cubeX - cubeZ;
        return new int[] {cubeX, cubeY, cubeZ};
    }
}
