package org.aventyrs.core.campaign;

import java.util.List;
import java.util.UUID;

import org.aventyrs.core.sheet.IllegalOperationException;
import org.junit.jupiter.api.Test;

import static org.aventyrs.core.util.TranslatableMessages.INVALID_SESSION_TRANSITION;
import static org.aventyrs.core.util.TranslatableMessages.PROGRESSION_LOCKED_DURING_SESSION;
import static org.aventyrs.core.util.TranslatableMessages.SESSION_ALREADY_ONGOING;
import static org.aventyrs.core.util.TranslatableMessages.SESSION_ALREADY_OPEN;
import static org.aventyrs.core.util.TranslatableMessages.SESSION_NOT_FOUND;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CampaignTest {

    private static Campaign playThrough(final int sessions) {
        Campaign campaign = Campaign.create("Crônicas");
        for (int i = 1; i <= sessions; i++) {
            campaign.createSession();
            campaign.startSession(i);
            campaign.endSession(i);
        }
        return campaign;
    }

    @Test
    void sessionsAreNumberedInCreationOrder() {
        Campaign campaign = playThrough(2);

        Session third = campaign.createSession();

        assertEquals(3, third.getNumber());
        assertEquals(List.of(1, 2, 3), campaign.getSessions().stream().map(Session::getNumber).toList());
        assertEquals(SessionStatus.CREATED, third.getStatus());
    }

    @Test
    void aNewSessionIsRefusedWhileAnotherIsStillOpen() {
        Campaign campaign = Campaign.create("Crônicas");
        campaign.createSession();

        IllegalOperationException whileCreated = assertThrows(IllegalOperationException.class, campaign::createSession);
        assertEquals(SESSION_ALREADY_OPEN, whileCreated.getMessage());

        campaign.startSession(1);
        IllegalOperationException whileOngoing = assertThrows(IllegalOperationException.class, campaign::createSession);
        assertEquals(SESSION_ALREADY_OPEN, whileOngoing.getMessage());
    }

    @Test
    void progressionIsLockedOnlyWhileASessionIsOngoing() {
        Campaign campaign = Campaign.create("Crônicas");
        campaign.createSession();
        assertFalse(campaign.isProgressionLocked());
        assertDoesNotThrow(campaign::requireProgressionAllowed);

        campaign.startSession(1);
        assertTrue(campaign.isProgressionLocked());
        assertEquals(1, campaign.getOngoingSession().orElseThrow().getNumber());
        IllegalOperationException locked = assertThrows(IllegalOperationException.class, campaign::requireProgressionAllowed);
        assertEquals(PROGRESSION_LOCKED_DURING_SESSION, locked.getMessage());

        campaign.endSession(1);
        assertFalse(campaign.isProgressionLocked());
        assertDoesNotThrow(campaign::requireProgressionAllowed);
    }

    @Test
    void aCampaignWithNoSessionsIsNotLocked() {
        assertFalse(Campaign.create("Crônicas").isProgressionLocked());
    }

    @Test
    void anEndedSessionCannotBeRestarted() {
        Campaign campaign = playThrough(1);

        IllegalOperationException error = assertThrows(IllegalOperationException.class, () -> campaign.startSession(1));

        assertEquals(INVALID_SESSION_TRANSITION, error.getMessage());
        assertFalse(campaign.isProgressionLocked());
    }

    @Test
    void aSessionThatNeverStartedCannotEnd() {
        Campaign campaign = Campaign.create("Crônicas");
        campaign.createSession();

        IllegalOperationException error = assertThrows(IllegalOperationException.class, () -> campaign.endSession(1));

        assertEquals(INVALID_SESSION_TRANSITION, error.getMessage());
    }

    @Test
    void startingTheOngoingSessionAgainIsAnInvalidTransition() {
        Campaign campaign = Campaign.create("Crônicas");
        campaign.createSession();
        campaign.startSession(1);

        IllegalOperationException error = assertThrows(IllegalOperationException.class, () -> campaign.startSession(1));

        assertEquals(INVALID_SESSION_TRANSITION, error.getMessage());
    }

    @Test
    void aSecondSessionCannotStartWhileAnotherIsOngoing() {
        // Only reachable from stored state: createSession itself refuses a second open Sessão.
        Session ongoing = Session.of(UUID.randomUUID(), 1, SessionStatus.ONGOING);
        Session ended = Session.of(UUID.randomUUID(), 2, SessionStatus.ENDED);
        Campaign campaign = Campaign.of(UUID.randomUUID(), "Crônicas", List.of(ongoing, ended), List.of());

        IllegalOperationException error = assertThrows(IllegalOperationException.class, () -> campaign.startSession(2));

        assertEquals(SESSION_ALREADY_ONGOING, error.getMessage());
    }

    @Test
    void anUnknownSessionNumberIsRefused() {
        Campaign campaign = Campaign.create("Crônicas");

        IllegalOperationException error = assertThrows(IllegalOperationException.class, () -> campaign.startSession(1));

        assertEquals(SESSION_NOT_FOUND, error.getMessage());
    }

    @Test
    void participantsAreTrackedBySheetIdAtAnyTime() {
        Campaign campaign = Campaign.create("Crônicas");
        UUID sheetId = UUID.randomUUID();
        campaign.createSession();
        campaign.startSession(1);

        assertTrue(campaign.addParticipant(sheetId));
        assertFalse(campaign.addParticipant(sheetId));
        assertTrue(campaign.isParticipant(sheetId));

        assertTrue(campaign.removeParticipant(sheetId));
        assertFalse(campaign.isParticipant(sheetId));
    }

    @Test
    void rebuildingOrdersSessionsAndKeepsNumbering() {
        UUID id = UUID.randomUUID();
        UUID sheetId = UUID.randomUUID();
        Session second = Session.of(UUID.randomUUID(), 2, SessionStatus.ONGOING);
        Session first = Session.of(UUID.randomUUID(), 1, SessionStatus.ENDED);

        Campaign campaign = Campaign.of(id, "Crônicas", List.of(second, first), List.of(sheetId));

        assertEquals(id, campaign.getId());
        assertEquals(List.of(first, second), campaign.getSessions());
        assertTrue(campaign.isParticipant(sheetId));
        assertTrue(campaign.isProgressionLocked());
        campaign.endSession(2);
        assertEquals(3, campaign.createSession().getNumber());
    }

    @Test
    void rebuildingRefusesGapsInSessionNumbers() {
        List<Session> gapped = List.of(
                Session.of(UUID.randomUUID(), 1, SessionStatus.ENDED),
                Session.of(UUID.randomUUID(), 3, SessionStatus.ENDED));

        assertThrows(IllegalArgumentException.class,
                () -> Campaign.of(UUID.randomUUID(), "Crônicas", gapped, List.of()));
    }

    @Test
    void rebuildingRefusesMoreThanOneOpenSession() {
        List<Session> twoOpen = List.of(
                Session.of(UUID.randomUUID(), 1, SessionStatus.ONGOING),
                Session.of(UUID.randomUUID(), 2, SessionStatus.CREATED));

        assertThrows(IllegalArgumentException.class,
                () -> Campaign.of(UUID.randomUUID(), "Crônicas", twoOpen, List.of()));
    }

    @Test
    void theSessionListIsReadOnly() {
        Campaign campaign = playThrough(1);

        assertThrows(UnsupportedOperationException.class, () -> campaign.getSessions().clear());
        assertThrows(UnsupportedOperationException.class, () -> campaign.getParticipantSheetIds().clear());
    }
}
