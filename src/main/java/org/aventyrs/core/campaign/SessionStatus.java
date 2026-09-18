package org.aventyrs.core.campaign;

/**
 * Where a {@link Session} stands in its one-way lifecycle: {@code CREATED → ONGOING → ENDED}.
 * {@code ENDED} is final, and nothing moves backwards.
 */
public enum SessionStatus {
    CREATED,
    ONGOING,
    ENDED;

    /** Whether a Sessão in this status may move directly to next. */
    public boolean canTransitionTo(final SessionStatus next) {
        return switch (this) {
            case CREATED -> next == ONGOING;
            case ONGOING -> next == ENDED;
            case ENDED -> false;
        };
    }
}
