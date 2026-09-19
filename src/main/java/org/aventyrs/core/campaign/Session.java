package org.aventyrs.core.campaign;

import java.util.UUID;

import org.aventyrs.core.sheet.IllegalOperationException;

import lombok.Getter;
import lombok.NonNull;

import static org.aventyrs.core.util.TranslatableMessages.INVALID_SESSION_TRANSITION;

/**
 * One sitting of a {@link Campaign}, numbered from 1 in the order it was created. A fresh Sessão
 * is {@link SessionStatus#CREATED}; {@link #start()} and {@link #end()} are the only ways to move it,
 * and each refuses a transition {@link SessionStatus#canTransitionTo} doesn't allow.
 *
 * <p>Create one through {@link Campaign#createSession()}, which assigns the number. {@link
 * #of(UUID, int, SessionStatus)} exists for rebuilding a stored Sessão, the same way {@code
 * CharacterSheet.of(..., id)} does, and checks nothing beyond a positive number.
 */
@Getter
public class Session {

    @NonNull
    private final UUID id;
    private final int number;
    @NonNull
    private SessionStatus status;

    private Session(final UUID id, final int number, final SessionStatus status) {
        if (number < 1) {
            throw new IllegalArgumentException("Session number must be 1 or greater, got " + number);
        }
        this.id = id;
        this.number = number;
        this.status = status;
    }

    static Session create(final int number) {
        return new Session(UUID.randomUUID(), number, SessionStatus.CREATED);
    }

    public static Session of(@NonNull final UUID id, final int number, @NonNull final SessionStatus status) {
        return new Session(id, number, status);
    }

    /** CREATED → ONGOING. Prefer {@link Campaign#startSession(int)}, which also enforces "one ONGOING at a time". */
    void start() {
        transitionTo(SessionStatus.ONGOING);
    }

    /** ONGOING → ENDED. */
    void end() {
        transitionTo(SessionStatus.ENDED);
    }

    public boolean isOngoing() {
        return status == SessionStatus.ONGOING;
    }

    /** Created but not yet ended — the Sessão the Campanha is currently about to play or playing. */
    public boolean isOpen() {
        return status != SessionStatus.ENDED;
    }

    private void transitionTo(final SessionStatus next) {
        if (!status.canTransitionTo(next)) {
            throw new IllegalOperationException(INVALID_SESSION_TRANSITION);
        }
        status = next;
    }
}
