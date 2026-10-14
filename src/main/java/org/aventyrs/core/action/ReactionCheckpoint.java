package org.aventyrs.core.action;

import org.aventyrs.core.scene.grid.BoardPosition;
import org.aventyrs.core.sheet.CombatantSheet;

import java.util.List;

/**
 * Where along a route a movement provokes Defender o Perímetro, and from whom (core 0.1.6.17; table ruling
 * 2026-10-14): the first position at which each hostile's melee reach covers the mover. The move pauses here, the
 * Reações resolve, and it continues unless the mover can no longer move.
 *
 * @param routeIndex the position's index along the route — 0 is the start, provoking as the mover leaves it
 * @param at         that position
 * @param reactors   who may react there, each listed at only one checkpoint of the route
 */
public record ReactionCheckpoint(int routeIndex, BoardPosition at, List<CombatantSheet> reactors) {

    public ReactionCheckpoint {
        reactors = List.copyOf(reactors);
    }
}
