package org.aventyrs.core.character.services;

import lombok.NonNull;
import org.aventyrs.core.scene.grid.GridPosition;
import org.aventyrs.core.scene.grid.HexGrid;
import org.aventyrs.core.scene.grid.MovementMap;
import org.aventyrs.core.scene.grid.TerrainCell;
import org.aventyrs.core.scene.grid.TerrainKind;
import org.aventyrs.core.sheet.CombatantSheet;

import java.util.Optional;

public class ForcedMovementServiceImpl implements ForcedMovementService {

    @Override
    public PushResult push(@NonNull final CombatantSheet target, @NonNull final GridPosition pusherAt,
                           @NonNull final GridPosition targetAt, final int ud, @NonNull final MovementMap map) {
        GridPosition from = pusherAt;
        GridPosition current = targetAt;
        int moved = 0;
        int fall = 0;
        boolean stoppedShort = false;
        for (int i = 0; i < ud; i++) {
            Optional<GridPosition> next = HexGrid.beyond(from, current);
            if (next.isEmpty() || next.get().x() >= map.columns() || next.get().y() >= map.rows()
                    || blocks(map, target, current, next.get())) {
                stoppedShort = true;
                break;
            }
            int drop = map.cellAt(current).groundLevel() - map.cellAt(next.get()).groundLevel();
            from = current;
            current = next.get();
            moved++;
            if (drop > MovementTerrainService.MAXIMUM_STEP) {
                // Off a ledge: the push ends with the fall.
                fall = drop;
                stoppedShort = i < ud - 1;
                break;
            }
            if (map.cellAt(current).kind() == TerrainKind.WATER) {
                stoppedShort = i < ud - 1;
                break;
            }
        }
        return new PushResult(current, moved, stoppedShort, fall,
                moved > 0 && map.cellAt(current).kind() == TerrainKind.WATER);
    }

    /** An obstacle, anyone standing there, or a rise steeper than a step. */
    private static boolean blocks(final MovementMap map, final CombatantSheet target, final GridPosition current,
                                  final GridPosition next) {
        TerrainCell cell = map.cellAt(next);
        return cell.kind() == TerrainKind.OBSTACLE
                || !map.occupantsOf(next, target).isEmpty()
                || cell.groundLevel() - map.cellAt(current).groundLevel() > MovementTerrainService.MAXIMUM_STEP;
    }
}
