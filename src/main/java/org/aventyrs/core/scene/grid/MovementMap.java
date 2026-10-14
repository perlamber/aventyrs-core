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
                          Map<GridPosition, List<CombatantSheet>> occupants, Set<UUID> defeated,
                          Map<GridPosition, TerrainCell> cells) {

    public MovementMap {
        difficultTerrain = difficultTerrain == null ? Set.of() : Set.copyOf(difficultTerrain);
        occupants = occupants == null ? Map.of() : Map.copyOf(occupants);
        defeated = defeated == null ? Set.of() : Set.copyOf(defeated);
        cells = cells == null ? Map.of() : Map.copyOf(cells);
    }

    /** A board with no painted terrain beyond its Terreno Difícil — every board before core 0.1.6.16. */
    public MovementMap(int columns, int rows, Set<GridPosition> difficultTerrain,
                       Map<GridPosition, List<CombatantSheet>> occupants, Set<UUID> defeated) {
        this(columns, rows, difficultTerrain, occupants, defeated, Map.of());
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

    /** Whether hex was marked Terreno Difícil on the board itself — in the legacy set or on its painted cell. */
    public boolean isDifficultTerrain(final GridPosition hex) {
        return difficultTerrain.contains(hex) || cellAt(hex).difficult();
    }

    /** The terrain painted on hex — plain ground at the baseline when nothing was. */
    public TerrainCell cellAt(final GridPosition hex) {
        return cells.getOrDefault(hex, TerrainCell.GROUND);
    }

    /** Everyone standing on hex other than mover. */
    public List<CombatantSheet> occupantsOf(final GridPosition hex, final CombatantSheet mover) {
        return occupants.getOrDefault(hex, List.of()).stream()
                .filter(sheet -> mover == null || !sheet.getId().equals(mover.getId()))
                .toList();
    }

    /**
     * Whether a straight line from from to to crosses nothing — "livre de barreiras, obstáculos e outros personagens"
     * (Investida, Encontrão). Every hex strictly between the two must be ground or water that is not difficult, not an
     * obstacle and empty of anyone but mover; the two ends are not judged.
     */
    public boolean isLineClear(final GridPosition from, final GridPosition to, final CombatantSheet mover) {
        List<GridPosition> line = HexGrid.line(from, to);
        for (int i = 1; i < line.size() - 1; i++) {
            GridPosition hex = line.get(i);
            TerrainCell cell = cellAt(hex);
            if (cell.kind() == TerrainKind.OBSTACLE || isDifficultTerrain(hex) || !occupantsOf(hex, mover).isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
