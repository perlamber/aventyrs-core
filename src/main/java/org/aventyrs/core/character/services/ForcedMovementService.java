package org.aventyrs.core.character.services;

import org.aventyrs.core.scene.grid.GridPosition;
import org.aventyrs.core.scene.grid.MovementMap;
import org.aventyrs.core.sheet.CombatantSheet;

/**
 * Being pushed (core 0.1.6.19) — "alvo é Empurrado 1UD para trás" (Encontrão), and the Corrente Rugido's push, which
 * core used to report and never apply. Straight away from whoever pushed, one hex at a time.
 *
 * <p>Readings approved with the 2026-10-14 plan: a push stops short at an obstacle, an occupied hex, a rise steeper
 * than a step, or the board's edge; one carried off a ledge falls the drop ({@code FallService}); one carried into
 * water lands swimming. Positions stay the caller's: this reports where the target ends up and the caller moves it.
 */
public interface ForcedMovementService {

    /**
     * Pushes target, standing at targetAt, ud hexes away from pusherAt across map.
     *
     * @return where it ends up and what happened on the way
     */
    PushResult push(CombatantSheet target, GridPosition pusherAt, GridPosition targetAt, int ud, MovementMap map);

    /**
     * Where a push ended.
     *
     * @param landing     the hex the target ends on — targetAt when it could not move at all
     * @param movedUd     how many hexes it travelled
     * @param stoppedShort whether something stopped it before the full distance
     * @param fallUd      how far it dropped off a ledge, 0 when it did not
     * @param intoWater   whether it ended in water
     */
    record PushResult(GridPosition landing, int movedUd, boolean stoppedShort, int fallUd, boolean intoWater) {
    }
}
