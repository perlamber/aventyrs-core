package org.aventyrs.core.scene.grid;

import java.util.List;

/**
 * One path a mover may take: the hexes stepped into, in order (the origin excluded, so an empty
 * list is standing still), what they cost in UD, and whether any of them was Terreno Difícil —
 * which refuses an Investida or a Reposicionar outright, whatever it cost.
 */
public record MovementPath(List<GridPosition> steps, int cost, boolean touchesDifficultTerrain) {

    public MovementPath {
        steps = List.copyOf(steps);
    }

    /** Where the path ends, or {@code null} for a path that goes nowhere. */
    public GridPosition destination() {
        return steps.isEmpty() ? null : steps.get(steps.size() - 1);
    }
}
