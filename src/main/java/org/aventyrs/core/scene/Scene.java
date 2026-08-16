package org.aventyrs.core.scene;

import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.aventyrs.core.util.TranslatableMessages.CHARACTER_SHEET_NOT_IN_SCENE;
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
 *
 * <p>{@link #terrainType} is the first bit of that "more Scene-scoped state" this class's own
 * javadoc used to only predict — a single value for the whole Scene (see {@link TerrainType}'s
 * own javadoc for why it isn't per-participant), {@code null} until a caller sets it via
 * {@link #setTerrainType} once the party's actual surroundings are known. {@link #buildContext}
 * carries it into the {@link SceneContext} snapshot handed to an {@code Interaction}, e.g. for
 * {@code AnoesRacialAbility#FILHOS_DA_MONTANHA}.
 *
 * <p>{@link #combatScene} is the other predicted bit of Scene-scoped state, now real: {@code
 * false} until a caller sets it via {@link #setCombatScene} once combat actually breaks out —
 * a Cena starts as a plain Cena and only becomes a Cena de Combate at that point, matching how
 * rules text like {@code InitiativeAdvantage#IMPETO}'s "nas duas primeiras Rodadas de cada Cena
 * de Combate" only applies once one has begun. Paired with {@link #getCurrentRound()} via
 * {@link SceneContext#isWithinFirstCombatRounds}, and with {@link #wonInitiative} for whichever
 * Vantagens further condition themselves on having won initiative.
 */
public class Scene {
    private final List<InitiativeEntry> activeEntries = new ArrayList<>();
    private final List<InitiativeEntry> pendingEntries = new ArrayList<>();

    private int currentIndex = -1;
    private int currentRound = 0;
    private TerrainType terrainType;
    private boolean combatScene;

    /**
     * Adds a CharacterSheet with its rolled initiative value, in a sub-group of its own —
     * equivalent to {@code addParticipant(characterSheet, initiativeValue, UUID.randomUUID())},
     * so it starts with no allies (see {@link #getAllies}) unless added via the other
     * overload with an explicit shared group. Before {@link #next()} has ever been called,
     * it's inserted directly into the current order at its sorted position. Afterwards —
     * this Scene already mid-rotation — it's held back and only joins the rotation, at its
     * sorted position, from the next Round onward; it never interrupts the Round currently
     * in progress.
     * @return the CharacterSheets in Iniciativa order after this addition
     */
    public List<CharacterSheet> addParticipant(final CharacterSheet characterSheet, final int initiativeValue) {
        return addParticipant(characterSheet, initiativeValue, UUID.randomUUID());
    }

    /**
     * Same as {@link #addParticipant(CharacterSheet, int)}, but placing characterSheet in
     * group — every other participant sharing that same group is this one's ally, per
     * {@link #getAllies}. Pass the same {@code UUID} to every CharacterSheet that should
     * consider each other allies (e.g. a party of PCs, or a pack of enemies).
     * @return the CharacterSheets in Iniciativa order after this addition
     */
    public List<CharacterSheet> addParticipant(final CharacterSheet characterSheet, final int initiativeValue, final UUID group) {
        InitiativeEntry entry = new InitiativeEntry(characterSheet, initiativeValue, group);
        if (currentIndex == -1) {
            insertSorted(activeEntries, entry);
        } else {
            pendingEntries.add(entry);
        }
        return getParticipantsInInitiativeOrder();
    }

    /**
     * Every other CharacterSheet sharing characterSheet's sub-group in this Scene — the
     * allies a character acting would consider (e.g. for {@code ArtesCompetencyAbility
     * .DOM_BARDICO}'s "concede... a eles, mas não a você" targeting), excluding
     * characterSheet itself. Searches both {@link #activeEntries} and {@link #pendingEntries},
     * since sub-group membership isn't a turn-order concern — an ally added mid-Round is
     * still an ally before it joins the rotation.
     * @throws IllegalOperationException if characterSheet was never added to this Scene
     */
    public List<CharacterSheet> getAllies(final CharacterSheet characterSheet) {
        UUID group = groupOf(characterSheet);
        return allEntries()
                .filter(entry -> entry.getGroup().equals(group))
                .map(InitiativeEntry::getCharacterSheet)
                .filter(sheet -> !sheet.getId().equals(characterSheet.getId()))
                .collect(Collectors.toList());
    }

    /**
     * Every participant in this Scene *not* sharing characterSheet's sub-group — the
     * complement of {@link #getAllies}. This is a simplification: with more than two
     * sub-groups in the same Scene (e.g. two feuding NPC factions plus the PCs), "not my
     * group" and "hostile to me" aren't necessarily the same thing, but this core has no
     * faction-relationship/allegiance concept beyond the binary "same group or not" — see
     * {@link SceneContext}, the consumer this method and {@link #getAllies} exist for.
     * @throws IllegalOperationException if characterSheet was never added to this Scene
     */
    public List<CharacterSheet> getEnemies(final CharacterSheet characterSheet) {
        UUID group = groupOf(characterSheet);
        return allEntries()
                .filter(entry -> !entry.getGroup().equals(group))
                .map(InitiativeEntry::getCharacterSheet)
                .collect(Collectors.toList());
    }

    /**
     * Builds a {@link SceneContext} snapshotting characterSheet's current allies/enemies in
     * this Scene (via {@link #getAllies}/{@link #getEnemies}), paired with distances —
     * {@code SceneContext} itself doesn't hold a {@code Scene} reference (see its own
     * javadoc for why), so this is the convenience for the common case of already having one.
     * @throws IllegalOperationException if characterSheet was never added to this Scene
     */
    public SceneContext buildContext(final CharacterSheet characterSheet, final Map<CharacterSheet, Range> distances) {
        return new SceneContext(getAllies(characterSheet), getEnemies(characterSheet), distances, terrainType,
                combatScene, currentRound, wonInitiative(characterSheet));
    }

    /** The kind of environment this Scene is currently taking place in, or {@code null} if never set. */
    public TerrainType getTerrainType() {
        return terrainType;
    }

    /** Sets this Scene's current terrain — e.g. once the party actually enters a cave. */
    public void setTerrainType(final TerrainType terrainType) {
        this.terrainType = terrainType;
    }

    /** Whether this Scene is currently a Cena de Combate. {@code false} until a caller sets it. */
    public boolean isCombatScene() {
        return combatScene;
    }

    /** Sets whether this Scene is currently a Cena de Combate — e.g. once combat actually breaks out. */
    public void setCombatScene(final boolean combatScene) {
        this.combatScene = combatScene;
    }

    /**
     * Whether characterSheet's sub-group currently holds this Scene's own highest rolled
     * Iniciativa — "ganhou a iniciativa," the condition several Vantagens de Iniciativa key
     * off (e.g. {@code InitiativeAdvantage#IMPETO}). A sub-group's own Iniciativa "value" is
     * the highest individual {@link InitiativeEntry#getInitiativeValue()} among its members —
     * matching how a party typically acts as a block on whichever single member rolled best —
     * compared against every other sub-group's own highest value; a tie for the overall
     * highest is considered a win for every sub-group sharing it, since the rules text this
     * models names no tie-breaker.
     * @throws IllegalOperationException if characterSheet was never added to this Scene
     */
    public boolean wonInitiative(final CharacterSheet characterSheet) {
        UUID group = groupOf(characterSheet);
        int groupBest = bestInitiativeValue(entry -> entry.getGroup().equals(group));
        int overallBest = bestInitiativeValue(entry -> true);
        return groupBest >= overallBest;
    }

    private int bestInitiativeValue(final Predicate<InitiativeEntry> filter) {
        return allEntries()
                .filter(filter)
                .mapToInt(InitiativeEntry::getInitiativeValue)
                .max()
                .orElseThrow(() -> new IllegalOperationException(NO_PARTICIPANTS_IN_SCENE));
    }

    private UUID groupOf(final CharacterSheet characterSheet) {
        return allEntries()
                .filter(entry -> entry.getCharacterSheet().getId().equals(characterSheet.getId()))
                .map(InitiativeEntry::getGroup)
                .findFirst()
                .orElseThrow(() -> new IllegalOperationException(CHARACTER_SHEET_NOT_IN_SCENE));
    }

    private Stream<InitiativeEntry> allEntries() {
        return Stream.concat(activeEntries.stream(), pendingEntries.stream());
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
