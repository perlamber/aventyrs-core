package org.aventyrs.core.scene.grid;

/**
 * Where a token stands in three dimensions: its hex, and its signed elevation in UD (table ruling, 2026-10-14) —
 * above the board's baseline when flying or climbing, below it when under water. A token on the ground stands at its
 * hex's {@link TerrainCell#groundLevel()}.
 */
public record BoardPosition(GridPosition hex, int elevation) {

    /** On hex, at the baseline. */
    public static BoardPosition at(final GridPosition hex) {
        return new BoardPosition(hex, 0);
    }
}
