package org.aventyrs.core.scene.grid;

/**
 * A single hex on a Scene's grid — flat-top hexagons, "even-q" offset coordinates: {@code x}
 * is the column, {@code y} the row; even columns sit at the grid's baseline row, odd columns
 * are visually shifted down by half a cell. This core otherwise has no positioning system of
 * its own ({@link org.aventyrs.core.scene.Range} is always caller-supplied) — this package is
 * the one opt-in way to derive one. See {@link HexGrid} for why offset coordinates need their
 * own distance formula instead of plain row/column math.
 *
 * <p>The grid is a fixed {@value #GRID_SIZE}x{@value #GRID_SIZE}, positive-only.
 */
public record GridPosition(int x, int y) {

    public static final int GRID_SIZE = 100;

    public GridPosition {
        if (x < 0 || x >= GRID_SIZE || y < 0 || y >= GRID_SIZE) {
            throw new IllegalArgumentException(
                    "Grid position out of bounds [0," + (GRID_SIZE - 1) + "]: (" + x + "," + y + ")");
        }
    }
}
