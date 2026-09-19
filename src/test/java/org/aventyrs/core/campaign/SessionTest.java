package org.aventyrs.core.campaign;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionTest {

    @Test
    void onlyTheForwardTransitionsAreAllowed() {
        assertTrue(SessionStatus.CREATED.canTransitionTo(SessionStatus.ONGOING));
        assertTrue(SessionStatus.ONGOING.canTransitionTo(SessionStatus.ENDED));

        assertFalse(SessionStatus.CREATED.canTransitionTo(SessionStatus.ENDED));
        assertFalse(SessionStatus.ONGOING.canTransitionTo(SessionStatus.CREATED));
        assertFalse(SessionStatus.ONGOING.canTransitionTo(SessionStatus.ONGOING));
    }

    @ParameterizedTest
    @EnumSource(SessionStatus.class)
    void endedIsFinal(final SessionStatus next) {
        assertFalse(SessionStatus.ENDED.canTransitionTo(next));
    }

    @Test
    void startAndEndWalkTheLifecycle() {
        Session session = Session.create(1);
        assertEquals(SessionStatus.CREATED, session.getStatus());
        assertTrue(session.isOpen());

        session.start();
        assertTrue(session.isOngoing());
        assertTrue(session.isOpen());

        session.end();
        assertEquals(SessionStatus.ENDED, session.getStatus());
        assertFalse(session.isOngoing());
        assertFalse(session.isOpen());
    }

    @Test
    void ofRebuildsAStoredSession() {
        UUID id = UUID.randomUUID();

        Session session = Session.of(id, 4, SessionStatus.ONGOING);

        assertEquals(id, session.getId());
        assertEquals(4, session.getNumber());
        assertTrue(session.isOngoing());
    }

    @Test
    void numbersStartAtOne() {
        assertThrows(IllegalArgumentException.class, () -> Session.of(UUID.randomUUID(), 0, SessionStatus.CREATED));
    }
}
