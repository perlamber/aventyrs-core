package org.aventyrs.core.scene;

import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.aventyrs.core.util.TranslatableMessages.NO_PARTICIPANTS_IN_SCENE;

/**
 * A Cena: the scope many rules key off (e.g. "uma vez a cada Cena", "ao longo da Cena").
 * Ordering its participating CharacterSheets by Iniciativa, and cycling through that order
 * turn by turn, is the first responsibility modeled here — more Scene-scoped state (e.g.
 * whether it's a Cena de Combate) is expected to land here over time.
 *
 * <p>A Scene can be created empty — CharacterSheets and their rolled initiative value are
 * added afterwards, in any order, as they become known, not necessarily when this entity
 * itself is instantiated. It's meant to be long-lived across an entire game session (its
 * order is decided once), not recreated per in-fiction scene: new enemies or helpers
 * encountered along the way just join it via {@link #addParticipant}.
 */
public class Scene {
    private final List<InitiativeEntry> activeEntries = new ArrayList<>();
    private final List<InitiativeEntry> pendingEntries = new ArrayList<>();

    private int currentIndex = -1;
    private int currentRound = 0;

    /**
     * Adds a CharacterSheet with its rolled initiative value. Before {@link #next()} has
     * ever been called, it's inserted directly into the current order at its sorted
     * position. Afterwards — this Scene already mid-rotation — it's held back and only
     * joins the rotation, at its sorted position, from the next Round onward; it never
     * interrupts the Round currently in progress.
     * @return the CharacterSheets in Iniciativa order after this addition
     */
    public List<CharacterSheet> addParticipant(final CharacterSheet characterSheet, final int initiativeValue) {
        InitiativeEntry entry = new InitiativeEntry(characterSheet, initiativeValue);
        if (currentIndex == -1) {
            insertSorted(activeEntries, entry);
        } else {
            pendingEntries.add(entry);
        }
        return getParticipantsInInitiativeOrder();
    }

    /**
     * The next CharacterSheet in Iniciativa order, advancing this Scene's turn cursor.
     * Wraps back to the top once every participant has acted, which also advances
     * {@link #getCurrentRound()} and merges in any participant added mid-Round.
     * @throws IllegalOperationException if no participant has been added yet
     */
    public CharacterSheet next() {
        if (activeEntries.isEmpty()) {
            throw new IllegalOperationException(NO_PARTICIPANTS_IN_SCENE);
        }
        currentIndex++;
        if (currentIndex >= activeEntries.size()) {
            currentIndex = 0;
            currentRound++;
            mergePendingEntries();
        }
        return activeEntries.get(currentIndex).getCharacterSheet();
    }

    /**
     * Which Round this Scene is currently on. 0-based: 0 is the first Round, same
     * convention as the turnNumber used across {@code ActionPointsService}.
     */
    public int getCurrentRound() {
        return currentRound;
    }

    /**
     * CharacterSheets currently in Iniciativa order — highest initiative value first, ties
     * kept in the order they were added. Doesn't include participants added mid-Round that
     * haven't joined the rotation yet; see {@link #addParticipant}.
     */
    public List<CharacterSheet> getParticipantsInInitiativeOrder() {
        return activeEntries.stream()
                .map(InitiativeEntry::getCharacterSheet)
                .collect(Collectors.toList());
    }

    private void mergePendingEntries() {
        for (InitiativeEntry pending : pendingEntries) {
            insertSorted(activeEntries, pending);
        }
        pendingEntries.clear();
    }

    /** Inserts before the first entry with a strictly lower value, keeping ties in insertion order. */
    private void insertSorted(final List<InitiativeEntry> sortedEntries, final InitiativeEntry entry) {
        int insertionIndex = sortedEntries.size();
        for (int i = 0; i < sortedEntries.size(); i++) {
            if (sortedEntries.get(i).getInitiativeValue() < entry.getInitiativeValue()) {
                insertionIndex = i;
                break;
            }
        }
        sortedEntries.add(insertionIndex, entry);
    }
}
