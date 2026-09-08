package org.aventyrs.core.scene;

/**
 * One of the four cardinal directions along which a {@link Scene} can be connected to a
 * neighbouring Scene. This core models scene-to-scene adjacency as a plain 4-way graph — no
 * diagonals, no distance, no pathfinding — that a caller walks one step at a time via
 * {@link Scene#getConnection(Direction)}.
 */
public enum Direction {
    NORTH,
    SOUTH,
    EAST,
    WEST;

    /**
     * The direction facing back the other way — the side the neighbouring Scene connects along
     * in return. {@link Scene#connectTo(Direction, Scene)} / {@link Scene#disconnect(Direction)}
     * keep the two sides consistent with this, so a caller never mirrors a link by hand.
     */
    public Direction opposite() {
        return switch (this) {
            case NORTH -> SOUTH;
            case SOUTH -> NORTH;
            case EAST -> WEST;
            case WEST -> EAST;
        };
    }
}
