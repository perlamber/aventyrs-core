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

    /**
     * Distance in three dimensions (table ruling, 2026-10-14): the larger of the hex distance and the elevation
     * difference — what reach and range read, so a flyer 3UD up is 3UD from the foe beneath it.
     */
    public static int distance3d(final BoardPosition a, final BoardPosition b) {
        return Math.max(distance(a.hex(), b.hex()), Math.abs(a.elevation() - b.elevation()));
    }

    /**
     * The hexes a straight line from a to b crosses, both ends included, in order — the standard cube-coordinate
     * interpolation, rounding each sample to its nearest hex (nudged off exact ties so the same pair always yields the
     * same line).
     */
    public static List<GridPosition> line(final GridPosition a, final GridPosition b) {
        int steps = distance(a, b);
        List<GridPosition> line = new ArrayList<>(steps + 1);
        int[] cubeA = toCube(a);
        int[] cubeB = toCube(b);
        for (int i = 0; i <= steps; i++) {
            double t = steps == 0 ? 0.0 : (double) i / steps;
            double x = cubeA[0] + (cubeB[0] - cubeA[0]) * t + 1e-6;
            double y = cubeA[1] + (cubeB[1] - cubeA[1]) * t + 1e-6;
            double z = cubeA[2] + (cubeB[2] - cubeA[2]) * t - 2e-6;
            GridPosition hex = fromCube(roundCube(x, y, z));
            if (line.isEmpty() || !line.get(line.size() - 1).equals(hex)) {
                line.add(hex);
            }
        }
        return line;
    }

    /**
     * The hex one step beyond through, continuing the straight direction from from through it — where a push carries a
     * target (core 0.1.6.19). For a from more than one hex away the direction is the line's last step. Empty when that
     * hex is off the grid, or when from and through coincide.
     */
    public static java.util.Optional<GridPosition> beyond(final GridPosition from, final GridPosition through) {
        List<GridPosition> line = line(from, through);
        if (line.size() < 2) {
            return java.util.Optional.empty();
        }
        int[] previous = toCube(line.get(line.size() - 2));
        int[] current = toCube(through);
        int[] next = {2 * current[0] - previous[0], 2 * current[1] - previous[1], 2 * current[2] - previous[2]};
        int x = next[0];
        int y = next[2] + x / 2;
        if (x < 0 || y < 0 || x >= GridPosition.GRID_SIZE || y >= GridPosition.GRID_SIZE) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(new GridPosition(x, y));
    }

    private static int[] roundCube(final double x, final double y, final double z) {
        long rx = Math.round(x);
        long ry = Math.round(y);
        long rz = Math.round(z);
        double dx = Math.abs(rx - x);
        double dy = Math.abs(ry - y);
        double dz = Math.abs(rz - z);
        if (dx > dy && dx > dz) {
            rx = -ry - rz;
        } else if (dy > dz) {
            ry = -rx - rz;
        } else {
            rz = -rx - ry;
        }
        return new int[] {(int) rx, (int) ry, (int) rz};
    }

    private static GridPosition fromCube(final int[] cube) {
        int x = cube[0];
        int y = cube[2] + x / 2;
        return new GridPosition(x, y);
    }

    private static int[] toCube(GridPosition position) {
        int cubeX = position.x();
        int cubeZ = position.y() - position.x() / 2;
        int cubeY = -cubeX - cubeZ;
        return new int[] {cubeX, cubeY, cubeZ};
    }
}
