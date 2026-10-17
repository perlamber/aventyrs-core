package org.aventyrs.core.action;

/**
 * Where a route begins among its mover's movements this Rodada — what a clause scoped to part of one movement reads
 * ({@link ActionProfile#CONSCIENCIA_DEFENSIVA}'s "primeiro movimento de cada Rodada, nos 2 primeiros UD percorridos").
 * This core records no movement's path, so the caller says how far into that movement the mover already is.
 *
 * @param movementIndex  the movement of the Rodada the route starts in, 0-based like {@code
 *                       CombatantSheet#getMovementsTakenThisRound()}
 * @param stepsTravelled hexes of that movement already travelled before the route starts — a move split over
 *                       several board clicks
 */
public record MovementProgress(int movementIndex, int stepsTravelled) {

    public MovementProgress {
        if (movementIndex < 0 || stepsTravelled < 0) {
            throw new IllegalArgumentException("movementIndex and stepsTravelled must not be negative");
        }
    }

    /** A movement starting fresh — none of it travelled yet. */
    public static MovementProgress startOf(final int movementIndex) {
        return new MovementProgress(movementIndex, 0);
    }
}
