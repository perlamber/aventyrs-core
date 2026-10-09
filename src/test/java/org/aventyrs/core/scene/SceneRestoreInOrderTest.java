package org.aventyrs.core.scene;

import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** {@link Scene#restoreParticipantInOrder}: a rebuild that keeps the stored order instead of re-sorting it. */
class SceneRestoreInOrderTest {

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static CharacterSheet sheet() {
        return CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK).build(), new Player());
    }

    @Test
    void theRotationKeepsTheOrderItWasRestoredIn() {
        CharacterSheet low = sheet();
        CharacterSheet high = sheet();
        CharacterSheet middle = sheet();
        Scene scene = new Scene();

        scene.restoreParticipantInOrder(low, 3, UUID.randomUUID());
        scene.restoreParticipantInOrder(high, 20, UUID.randomUUID());
        scene.restoreParticipantInOrder(middle, 10, UUID.randomUUID());

        assertEquals(List.of(low, high, middle), scene.getParticipantsInInitiativeOrder());
    }

    @Test
    void aSheetsOwnIniciativaBonusDoesNotMoveItInTheRestoredOrder() {
        CharacterSheet boosted = sheet();
        CharacterSheet plain = sheet();
        boosted.grantTemporaryBonus(ModifierType.INITIATIVE, 50, 3);
        Scene scene = new Scene();

        scene.restoreParticipantInOrder(plain, 10, UUID.randomUUID());
        scene.restoreParticipantInOrder(boosted, 10, UUID.randomUUID());

        assertEquals(List.of(plain, boosted), scene.getParticipantsInInitiativeOrder());
    }

    @Test
    void whoeverIsRestoredAfterTheCursorStillWaitsForTheNextRodada() {
        CharacterSheet first = sheet();
        CharacterSheet second = sheet();
        CharacterSheet waiting = sheet();
        Scene scene = new Scene();
        scene.setCombatScene(true);
        scene.restoreParticipantInOrder(first, 5, UUID.randomUUID());
        scene.restoreParticipantInOrder(second, 15, UUID.randomUUID());
        scene.restoreTurnCursor(2, 1);

        scene.restoreParticipantInOrder(waiting, 30, UUID.randomUUID());

        assertEquals(List.of(first, second), scene.getParticipantsInInitiativeOrder());
        assertEquals(List.of(waiting), scene.getPendingParticipants());
        assertEquals(waiting, scene.next());
        assertEquals(3, scene.getCurrentRound());
        // The Rodada boundary is core's own: it merges the waiting one and sorts by Iniciativa again.
        assertEquals(List.of(waiting, second, first), scene.getParticipantsInInitiativeOrder());
    }
}
