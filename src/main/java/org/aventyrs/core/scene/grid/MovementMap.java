package org.aventyrs.core.scene.grid;

import org.aventyrs.core.sheet.CombatantSheet;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The board as a movement sees it: its extent, which hexes are Terreno Difícil, and who stands
 * where. Supplied whole by the caller — this core holds no positions — and read by {@code
 * org.aventyrs.core.character.services.MovementTerrainService} to build a mover's {@link
 * StepRules}.
 *
 * <p>{@code occupants} may list several sheets on one hex (Entre as Pernas lets two share one);
 * the mover's own entry, if present, is ignored.
 */
public record MovementMap(int columns, int rows, Set<GridPosition> difficultTerrain,
                          Map<GridPosition, List<CombatantSheet>> occupants) {

    public MovementMap {
        difficultTerrain = difficultTerrain == null ? Set.of() : Set.copyOf(difficultTerrain);
        occupants = occupants == null ? Map.of() : Map.copyOf(occupants);
    }

    /** Whether hex was marked Terreno Difícil on the board itself. */
    public boolean isDifficultTerrain(final GridPosition hex) {
        return difficultTerrain.contains(hex);
    }

    /** Everyone standing on hex other than mover. */
    public List<CombatantSheet> occupantsOf(final GridPosition hex, final CombatantSheet mover) {
        return occupants.getOrDefault(hex, List.of()).stream()
                .filter(sheet -> mover == null || !sheet.getId().equals(mover.getId()))
                .toList();
    }
}
