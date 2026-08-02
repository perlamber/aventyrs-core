package org.aventyrs.core.scene;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SceneTest {

    @BeforeEach
    public void setup() {
        CharacterFixture.loadTemplates();
    }

    private CharacterSheet newSheet() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();
        return CharacterSheet.of(character, new Player());
    }

    @Test
    void canBeCreatedEmptyWithNoParticipantsYet() {
        Scene scene = new Scene();
        assertTrue(scene.getParticipantsInInitiativeOrder().isEmpty());
    }

    @Test
    void ordersParticipantsByDescendingInitiativeValue() {
        Scene scene = new Scene();
        CharacterSheet slow = newSheet();
        CharacterSheet fast = newSheet();
        CharacterSheet medium = newSheet();

        scene.addParticipant(slow, 5);
        scene.addParticipant(fast, 18);
        scene.addParticipant(medium, 11);

        assertEquals(List.of(fast, medium, slow), scene.getParticipantsInInitiativeOrder());
    }

    @Test
    void addParticipantReturnsTheUpdatedOrder() {
        Scene scene = new Scene();
        CharacterSheet first = newSheet();
        CharacterSheet second = newSheet();

        scene.addParticipant(first, 10);
        List<CharacterSheet> updated = scene.addParticipant(second, 15);

        assertEquals(List.of(second, first), updated);
    }

    @Test
    void tiedInitiativeValuesKeepTheOrderTheyWereAddedIn() {
        Scene scene = new Scene();
        CharacterSheet addedFirst = newSheet();
        CharacterSheet addedSecond = newSheet();

        scene.addParticipant(addedFirst, 10);
        scene.addParticipant(addedSecond, 10);

        assertEquals(List.of(addedFirst, addedSecond), scene.getParticipantsInInitiativeOrder());
    }

    @Test
    void nextThrowsWhenNoParticipantHasBeenAdded() {
        Scene scene = new Scene();
        assertThrows(IllegalOperationException.class, scene::next);
    }

    @Test
    void nextCyclesThroughParticipantsInInitiativeOrderThenWrapsAround() {
        Scene scene = new Scene();
        CharacterSheet slow = newSheet();
        CharacterSheet fast = newSheet();
        CharacterSheet medium = newSheet();
        scene.addParticipant(slow, 5);
        scene.addParticipant(fast, 18);
        scene.addParticipant(medium, 11);

        assertEquals(fast, scene.next());
        assertEquals(medium, scene.next());
        assertEquals(slow, scene.next());
        assertEquals(fast, scene.next());
    }

    @Test
    void currentRoundStartsAtZeroAndIncrementsEveryFullCycle() {
        Scene scene = new Scene();
        CharacterSheet first = newSheet();
        CharacterSheet second = newSheet();
        scene.addParticipant(first, 10);
        scene.addParticipant(second, 5);

        scene.next();
        assertEquals(0, scene.getCurrentRound());
        scene.next();
        assertEquals(0, scene.getCurrentRound());
        scene.next();
        assertEquals(1, scene.getCurrentRound());
    }

    @Test
    void participantAddedMidRoundDoesNotInterruptTheCurrentRound() {
        Scene scene = new Scene();
        CharacterSheet a = newSheet();
        CharacterSheet b = newSheet();
        CharacterSheet lateArrival = newSheet();
        scene.addParticipant(a, 10);
        scene.addParticipant(b, 5);

        assertEquals(a, scene.next());
        scene.addParticipant(lateArrival, 20);
        assertEquals(List.of(a, b), scene.getParticipantsInInitiativeOrder());
        assertEquals(b, scene.next());
    }

    @Test
    void participantAddedMidRoundJoinsAtItsSortedSlotStartingNextRound() {
        Scene scene = new Scene();
        CharacterSheet a = newSheet();
        CharacterSheet b = newSheet();
        CharacterSheet lateArrival = newSheet();
        scene.addParticipant(a, 10);
        scene.addParticipant(b, 5);

        scene.next();
        scene.addParticipant(lateArrival, 20);
        scene.next();

        assertEquals(lateArrival, scene.next());
        assertEquals(1, scene.getCurrentRound());
        assertEquals(List.of(lateArrival, a, b), scene.getParticipantsInInitiativeOrder());
    }
}
