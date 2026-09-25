package org.aventyrs.core.scene.grid;

import org.aventyrs.core.sheet.CombatantSheet;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The board as a movement sees it: its extent, which hexes are Terreno Difícil, and who stands
 * where. Supplied whole by the caller — this core holds no positions — and read by {@code
 * org.aventyrs.core.character.services.MovementTerrainService} to build a mover's {@link
 * StepRules}.
 *
 * <p>{@code occupants} may list several sheets on one hex (Entre as Pernas lets two share one);
 * the mover's own entry, if present, is ignored.
 *
 * <p>{@code defeated} names the occupants who are down ({@code LootService#isDefeated}: Caído, em
 * Coma or morto). The caller supplies it because it reads the status tier, which is all a client
 * knows of a remote foe. A defeated foe does not block a mover, and its space is Terreno Difícil.
 */
public record MovementMap(int columns, int rows, Set<GridPosition> difficultTerrain,
                          Map<GridPosition, List<CombatantSheet>> occupants, Set<UUID> defeated) {

    public MovementMap {
        difficultTerrain = difficultTerrain == null ? Set.of() : Set.copyOf(difficultTerrain);
        occupants = occupants == null ? Map.of() : Map.copyOf(occupants);
        defeated = defeated == null ? Set.of() : Set.copyOf(defeated);
    }

    /** A board on which nobody is down. */
    public MovementMap(int columns, int rows, Set<GridPosition> difficultTerrain,
                       Map<GridPosition, List<CombatantSheet>> occupants) {
        this(columns, rows, difficultTerrain, occupants, Set.of());
    }

    /** Whether occupant is down — Caído, em Coma or morto. */
    public boolean isDefeated(final CombatantSheet occupant) {
        return occupant != null && defeated.contains(occupant.getId());
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
