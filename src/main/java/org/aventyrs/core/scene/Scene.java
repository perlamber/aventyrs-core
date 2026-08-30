package org.aventyrs.core.scene;

import org.aventyrs.core.sheet.Blessing;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.TargetScope;
import org.aventyrs.core.sheet.TemporaryBonus;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.aventyrs.core.util.TranslatableMessages.CHARACTER_SHEET_NOT_IN_SCENE;
import static org.aventyrs.core.util.TranslatableMessages.INITIATIVE_NOT_WON;
import static org.aventyrs.core.util.TranslatableMessages.INVALID_TURN_CURSOR;
import static org.aventyrs.core.util.TranslatableMessages.NO_PARTICIPANTS_IN_SCENE;

/**
 * A Cena: the scope many rules key off (e.g. "uma vez a cada Cena", "ao longo da Cena").
 * Ordering its participating CombatantSheets by Iniciativa, and cycling through that order
 * turn by turn, is the first responsibility modeled here — more Scene-scoped state (e.g.
 * whether it's a Cena de Combate) is expected to land here over time.
 *
 * <p>A Scene can be created empty — CombatantSheets and their rolled initiative value are
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
 *
 * <p>{@link #applyInitiativeBlessings} is the caller-invoked hook for the moment a group
 * actually wins initiative: given the winner's own already-resolved {@link
 * org.aventyrs.core.sheet.Blessing}s (a caller resolves these itself via {@code
 * org.aventyrs.core.character.services.InitiativeBlessingService#resolveBlessings} — this
 * class only tracks/applies them, it never reaches into a Service to compute what a
 * Character's abilities grant, the same restraint {@link #buildContext} already applies to
 * every other fact it assembles), it grants all of them to the winner and, for whichever
 * apply to allies too, every current member of {@link #getAllies(CombatantSheet)} — tracked
 * in {@link #grantedBlessings} so a later call revokes exactly what an earlier one granted
 * before granting the new winner's own set, rather than leaving stale buffs behind on whoever
 * held them before. {@link #addParticipant} mirrors this for a CombatantSheet that joins an
 * *already*-blessed group afterwards — "blessed" isn't cached anywhere separately, a group is
 * blessed exactly when {@link #grantedBlessings} already tracks a member of it (see {@link
 * #isGroupBlessed}) — applying the same currently-active ally-scoped blessings to it
 * immediately.
 *
 * <p>{@link InitiativeEntry#getInitiativeValue()} is fixed once rolled, but a participant's
 * actual Iniciativa *standing* isn't necessarily fixed for their whole time in this Scene — a
 * bonus granted by another Character, an activated Ability, or a passive with a Round trigger
 * can each grant a Round-scoped {@code TemporaryBonus} targeting {@code
 * ModifierType.INITIATIVE} directly to a {@code CombatantSheet}, the same way any other
 * temporary bonus already is (see {@code CombatantSheet#grantTemporaryBonus}) — nothing calls
 * into this class to do that, and nothing needs to: {@link CombatantSheet} has no reference
 * back to a {@code Scene}. Instead, {@link InitiativeEntry#getEffectiveInitiativeValue()}
 * resolves the live total straight from the {@code CombatantSheet} each entry already
 * references, and this class reads *that* wherever current standing matters —
 * {@link #wonInitiative}/{@link #buildContext} reflect a change the instant it's granted, no
 * different from how they already recompute fresh on every call. This class's turn *order*
 * (what {@link #getParticipantsInInitiativeOrder()}/{@link #next()} walk) is the one thing kept
 * stable mid-Round on purpose — it's only ever re-derived, from every participant's current
 * {@link InitiativeEntry#getEffectiveInitiativeValue()}, at the same Round-boundary point a
 * pending newcomer already waits for (see {@link #next()}) — so a granted bonus can flip who's
 * currently winning immediately without reshuffling turns already in progress this Round.
 * {@link CombatantSheet#finishTurn()} — called by {@link #next()} on whoever's turn just ended
 * — is what advances a granted bonus toward expiry in the first place.
 *
 * <p>A Scene whose turn cursor lives somewhere else — persisted between sessions, or advanced by
 * a server every client mirrors — is rebuilt here in two passes rather than by replaying {@link
 * #next()} once per elapsed turn (which would re-fire every {@link CombatantSheet#startTurn(int)}
 * and {@link CombatantSheet#finishTurn()} along the way, ticking effects that already ticked).
 * First add everyone already in the rotation, while {@link #getCurrentIndex()} is still {@code -1}
 * and {@link #addParticipant} therefore inserts straight into the live order; then {@link
 * #restoreTurnCursor} to the round/index reached elsewhere; then add whoever joined mid-Round.
 * That last group lands in the pending set on its own, because {@link #addParticipant}'s existing
 * {@code currentIndex != -1} branch is now the one that applies — the split this class already
 * maintains is reproduced by the same code that maintains it, not by a second copy of the rule.
 */
public class Scene {
    private final List<InitiativeEntry> activeEntries = new ArrayList<>();
    private final List<InitiativeEntry> pendingEntries = new ArrayList<>();
    private final Map<CombatantSheet, List<TemporaryBonus>> grantedBlessings = new HashMap<>();

    private int currentIndex = -1;
    private int currentRound = 0;
    private TerrainType terrainType;
    private boolean combatScene;
    private List<Blessing> activeBlessings = List.of();

    /**
     * Adds a CombatantSheet with its rolled initiative value, in a sub-group of its own —
     * equivalent to {@code addParticipant(characterSheet, initiativeValue, UUID.randomUUID())},
     * so it starts with no allies (see {@link #getAllies}) unless added via the other
     * overload with an explicit shared group. Before {@link #next()} has ever been called,
     * it's inserted directly into the current order at its sorted position. Afterwards —
     * this Scene already mid-rotation — it's held back and only joins the rotation, at its
     * sorted position, from the next Round onward; it never interrupts the Round currently
     * in progress.
     * @return the CombatantSheets in Iniciativa order after this addition
     */
    public List<CombatantSheet> addParticipant(final CombatantSheet characterSheet, final int initiativeValue) {
        return addParticipant(characterSheet, initiativeValue, UUID.randomUUID());
    }

    /**
     * Same as {@link #addParticipant(CombatantSheet, int)}, but placing characterSheet in
     * group — every other participant sharing that same group is this one's ally, per
     * {@link #getAllies}. Pass the same {@code UUID} to every CombatantSheet that should
     * consider each other allies (e.g. a party of PCs, or a pack of enemies).
     *
     * <p>If group is currently blessed (see {@link #isGroupBlessed}) — i.e. {@link
     * #grantedBlessings} already tracks another member of it from an earlier {@link
     * #applyInitiativeBlessings} call — characterSheet immediately receives every currently-
     * active {@link TargetScope#SELF_AND_ALLIES}-scoped {@link Blessing} too — it's joining a
     * group that already won initiative, even though it didn't personally roll the highest
     * value itself. A {@link TargetScope#SELF}-only blessing never propagates this way, since
     * it belongs to whichever Character actually holds the granting ability, not the group at
     * large.
     * @return the CombatantSheets in Iniciativa order after this addition
     */
    public List<CombatantSheet> addParticipant(final CombatantSheet characterSheet, final int initiativeValue, final UUID group) {
        InitiativeEntry entry = new InitiativeEntry(characterSheet, initiativeValue, group);
        if (currentIndex == -1) {
            insertSorted(activeEntries, entry);
        } else {
            pendingEntries.add(entry);
        }
        if (isGroupBlessed(group)) {
            for (Blessing blessing : activeBlessings) {
                if (blessing.getScope() == TargetScope.SELF_AND_ALLIES) {
                    grantBlessing(characterSheet, blessing);
                }
            }
        }
        return getParticipantsInInitiativeOrder();
    }

    /**
     * Whether group is the sub-group {@link #applyInitiativeBlessings} most recently blessed —
     * derived from {@link #grantedBlessings} itself (true when any currently-tracked recipient
     * belongs to group) rather than cached in a separate field, so it can never drift out of
     * sync with what's actually granted.
     */
    private boolean isGroupBlessed(final UUID group) {
        return grantedBlessings.keySet().stream().anyMatch(sheet -> groupOf(sheet).equals(group));
    }

    /**
     * Every other CombatantSheet sharing characterSheet's sub-group in this Scene — the
     * allies a character acting would consider (e.g. for {@code ArtesCompetencyAbility
     * .DOM_BARDICO}'s "concede... a eles, mas não a você" targeting), excluding
     * characterSheet itself. Searches both {@link #activeEntries} and {@link #pendingEntries},
     * since sub-group membership isn't a turn-order concern — an ally added mid-Round is
     * still an ally before it joins the rotation.
     * @throws IllegalOperationException if characterSheet was never added to this Scene
     */
    public List<CombatantSheet> getAllies(final CombatantSheet characterSheet) {
        UUID group = groupOf(characterSheet);
        return allEntries()
                .filter(entry -> entry.getGroup().equals(group))
                .map(InitiativeEntry::getCombatantSheet)
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
    public List<CombatantSheet> getEnemies(final CombatantSheet characterSheet) {
        UUID group = groupOf(characterSheet);
        return allEntries()
                .filter(entry -> !entry.getGroup().equals(group))
                .map(InitiativeEntry::getCombatantSheet)
                .collect(Collectors.toList());
    }

    /**
     * Builds a {@link SceneContext} snapshotting characterSheet's current allies/enemies in
     * this Scene (via {@link #getAllies}/{@link #getEnemies}), paired with distances —
     * {@code SceneContext} itself doesn't hold a {@code Scene} reference (see its own
     * javadoc for why), so this is the convenience for the common case of already having one.
     * @throws IllegalOperationException if characterSheet was never added to this Scene
     */
    public SceneContext buildContext(final CombatantSheet characterSheet, final Map<CombatantSheet, Range> distances) {
        return buildContext(characterSheet, distances, null);
    }

    /**
     * Same as {@link #buildContext(CombatantSheet, Map)}, but also naming the combatant on the
     * other side of the roll this context is being built for — see {@code
     * SceneContext#getOpposedCharacter()} for which side that is (the target on an attack roll,
     * the attacker on a defence roll). Pass {@code null}, or use the shorter overload, for a
     * roll that opposes nobody.
     * @throws IllegalOperationException if characterSheet was never added to this Scene
     */
    public SceneContext buildContext(final CombatantSheet characterSheet, final Map<CombatantSheet, Range> distances,
                                      final CombatantSheet opposedCharacter) {
        return new SceneContext(getAllies(characterSheet), getEnemies(characterSheet), distances, terrainType,
                combatScene, currentRound, wonInitiative(characterSheet), opposedCharacter);
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
     * Whether characterSheet's sub-group currently holds this Scene's own highest Iniciativa
     * standing right now — "ganhou a iniciativa," the condition several Vantagens de Iniciativa
     * key off (e.g. {@code InitiativeAdvantage#IMPETO}). A sub-group's own Iniciativa "value" is
     * the highest individual {@link InitiativeEntry#getEffectiveInitiativeValue()} among its
     * members — matching how a party typically acts as a block on whichever single member
     * rolled best, and reflecting any Round-scoped Iniciativa bonus currently active on them —
     * compared against every other sub-group's own highest value; a tie for the overall
     * highest is considered a win for every sub-group sharing it, since the rules text this
     * models names no tie-breaker.
     * @throws IllegalOperationException if characterSheet was never added to this Scene
     */
    public boolean wonInitiative(final CombatantSheet characterSheet) {
        UUID group = groupOf(characterSheet);
        int groupBest = bestInitiativeValue(entry -> entry.getGroup().equals(group));
        int overallBest = bestInitiativeValue(entry -> true);
        return groupBest >= overallBest;
    }

    private int bestInitiativeValue(final Predicate<InitiativeEntry> filter) {
        return allEntries()
                .filter(filter)
                .mapToInt(InitiativeEntry::getEffectiveInitiativeValue)
                .max()
                .orElseThrow(() -> new IllegalOperationException(NO_PARTICIPANTS_IN_SCENE));
    }

    /**
     * The method to call the moment winner's group actually wins initiative (see
     * {@link #wonInitiative}) with blessings — winner's own already-resolved {@link
     * Blessing}s, e.g. from {@code
     * org.aventyrs.core.character.services.InitiativeBlessingService#resolveBlessings(winner
     * .getCharacter())}; this class deliberately doesn't resolve them itself, see this class's
     * own javadoc. Throws {@link IllegalOperationException} ({@code INITIATIVE_NOT_WON}) if
     * winner hasn't actually won. Revokes every blessing an earlier call granted first (via
     * {@link CombatantSheet#removeEffect}, precisely undoing what was tracked in {@link
     * #grantedBlessings} — never touching an unrelated {@code TemporaryBonus} of the same
     * {@code ModifierType} from some other source), then grants <em>all</em> of blessings to
     * winner directly, plus every {@link TargetScope#SELF_AND_ALLIES}-scoped one to each of
     * {@link #getAllies(CombatantSheet)} — each grant a fresh {@link TemporaryBonus}, tracked
     * so a later call (a new group winning, or the same one winning again) can revoke
     * precisely these. Also remembers blessings itself so {@link #addParticipant} can extend
     * the ally-scoped ones to whoever joins winner's group afterwards.
     * @throws IllegalOperationException if winner was never added to this Scene, or hasn't
     *                                    actually won initiative for its group
     */
    public void applyInitiativeBlessings(final CombatantSheet winner, final List<Blessing> blessings) {
        if (!wonInitiative(winner)) {
            throw new IllegalOperationException(INITIATIVE_NOT_WON);
        }
        revokeActiveBlessings();
        activeBlessings = blessings;

        for (Blessing blessing : blessings) {
            grantBlessing(winner, blessing);
            if (blessing.getScope() == TargetScope.SELF_AND_ALLIES) {
                for (CombatantSheet ally : getAllies(winner)) {
                    grantBlessing(ally, blessing);
                }
            }
        }
    }

    private void revokeActiveBlessings() {
        grantedBlessings.forEach((sheet, bonuses) -> bonuses.forEach(sheet::removeEffect));
        grantedBlessings.clear();
        activeBlessings = List.of();
    }

    private void grantBlessing(final CombatantSheet sheet, final Blessing blessing) {
        TemporaryBonus bonus = new TemporaryBonus(blessing.getModifierType(), blessing.getValue(), blessing.getRounds());
        sheet.applyEffect(bonus);
        grantedBlessings.computeIfAbsent(sheet, key -> new ArrayList<>()).add(bonus);
    }

    private UUID groupOf(final CombatantSheet characterSheet) {
        return allEntries()
                .filter(entry -> entry.getCombatantSheet().getId().equals(characterSheet.getId()))
                .map(InitiativeEntry::getGroup)
                .findFirst()
                .orElseThrow(() -> new IllegalOperationException(CHARACTER_SHEET_NOT_IN_SCENE));
    }

    private Stream<InitiativeEntry> allEntries() {
        return Stream.concat(activeEntries.stream(), pendingEntries.stream());
    }

    /**
     * The next CombatantSheet in Iniciativa order, advancing this Scene's turn cursor. First
     * calls {@link CombatantSheet#finishTurn()} on whoever's turn is ending (a no-op on the
     * very first call, before anyone has had a turn yet) — this is what advances any Round-scoped
     * {@code TemporaryBonus} that participant is holding (including one targeting {@code
     * ModifierType.INITIATIVE}) toward expiry. Wraps back to the top once every participant has
     * acted, which also advances {@link #getCurrentRound()} and calls {@link
     * #startNewRound()} — merging in any participant added mid-Round *and* re-deriving turn
     * order from everyone's current {@link InitiativeEntry#getEffectiveInitiativeValue()}, so a
     * granted/expired Iniciativa bonus is reflected in the order from the next Round onward,
     * never mid-Round. Finally calls {@link CombatantSheet#startTurn(int)} on whoever's turn is
     * now beginning, passing {@link #getCurrentRound()} as its turnNumber — unlike {@code
     * finishTurn()}, this fires even on the very first call, since that call does start
     * someone's Turn, just none has ended yet.
     * @throws IllegalOperationException if no participant has been added yet
     */
    public CombatantSheet next() {
        if (activeEntries.isEmpty()) {
            throw new IllegalOperationException(NO_PARTICIPANTS_IN_SCENE);
        }
        if (currentIndex >= 0) {
            activeEntries.get(currentIndex).getCombatantSheet().finishTurn();
        }
        currentIndex++;
        if (currentIndex >= activeEntries.size()) {
            currentIndex = 0;
            currentRound++;
            startNewRound();
        }
        CombatantSheet active = activeEntries.get(currentIndex).getCombatantSheet();
        active.startTurn(currentRound);
        return active;
    }

    /**
     * Which Round this Scene is currently on. 0-based: 0 is the first Round, same
     * convention as the turnNumber used across {@code ActionPointsService}.
     */
    public int getCurrentRound() {
        return currentRound;
    }

    /**
     * CombatantSheets currently in Iniciativa order — highest initiative value first, ties
     * kept in the order they were added. Doesn't include participants added mid-Round that
     * haven't joined the rotation yet; see {@link #addParticipant}.
     */
    public List<CombatantSheet> getParticipantsInInitiativeOrder() {
        return activeEntries.stream()
                .map(InitiativeEntry::getCombatantSheet)
                .collect(Collectors.toList());
    }

    /**
     * How far into {@link #getParticipantsInInitiativeOrder()} this Scene's turn cursor currently
     * sits — {@code -1} before {@link #next()} has ever been called, i.e. before anyone has acted.
     * Read-only counterpart to {@link #next()}, which is otherwise the only way to learn where the
     * cursor is and can't be used for the purpose without also moving it.
     */
    public int getCurrentIndex() {
        return currentIndex;
    }

    /**
     * Whoever's Turn it currently is, or {@code null} before {@link #next()} has ever been called.
     * Unlike {@link #next()} this never advances anything, never fires {@link
     * CombatantSheet#startTurn(int)}/{@link CombatantSheet#finishTurn()}, and can be called as
     * often as a caller likes — it's what something displaying the current Turn should read.
     */
    public CombatantSheet getCurrentCombatant() {
        if (currentIndex < 0 || currentIndex >= activeEntries.size()) {
            return null;
        }
        return activeEntries.get(currentIndex).getCombatantSheet();
    }

    /**
     * The CombatantSheets added mid-Round that haven't joined the rotation yet — the ones {@link
     * #getParticipantsInInitiativeOrder()} deliberately omits (see {@link #addParticipant}), in
     * the order they were added. They join, at their sorted positions, at the next Round boundary
     * {@link #next()} reaches. Empty whenever nothing is waiting.
     */
    public List<CombatantSheet> getPendingParticipants() {
        return pendingEntries.stream()
                .map(InitiativeEntry::getCombatantSheet)
                .collect(Collectors.toList());
    }

    /**
     * Everyone in this Scene — those in the rotation (in Iniciativa order) followed by those
     * added mid-Round and still waiting (in the order added). The union of {@link
     * #getParticipantsInInitiativeOrder()} and {@link #getPendingParticipants()}, which are
     * <strong>disjoint by construction</strong>: {@link #addParticipant} routes each entry to
     * exactly one of the two, {@link #startNewRound()} only ever moves pending into active, and
     * {@link #removeParticipant} searches both. So this needs no de-duplication, and no
     * participant can be missed by it.
     *
     * <p>This is what a caller wants when the question is "who is in this Scene" rather than
     * "whose Turn is it" — building an end-of-session roster, say. The other two accessors stay
     * because turn order and not-yet-joined-ness are real distinctions elsewhere; this one
     * exists so a caller asking the simpler question doesn't have to re-derive that the two
     * halves are disjoint before concatenating them.
     *
     * <p>A fresh copy, like both halves it's built from — mutating it doesn't disturb the Scene.
     */
    public List<CombatantSheet> getAllParticipants() {
        return allEntries()
                .map(InitiativeEntry::getCombatantSheet)
                .collect(Collectors.toList());
    }

    /**
     * Points this Scene's turn cursor at round/index without running any of the turn-boundary
     * behavior {@link #next()} does — no {@link CombatantSheet#startTurn(int)}, no {@link
     * CombatantSheet#finishTurn()}, no {@link #startNewRound()}. This is deliberate and is the
     * whole point of the method: it exists for a Scene being rebuilt around a cursor that was
     * already advanced somewhere else (loaded from persistence, or mirrored from whichever peer
     * actually called {@link #next()}), where those callbacks have already had their effect on
     * the participants involved and firing them again would tick every Round-scoped {@link
     * TemporaryBonus} a second time. Replaying turn lifecycle is explicitly not this method's
     * job — for actually taking a turn, call {@link #next()}.
     *
     * <p>index is bounds-checked against the participants added so far, so the two-pass rebuild
     * described in this class's own javadoc has to add the rotation before restoring the cursor,
     * not after.
     * @throws IllegalOperationException if round is negative, or index is outside
     *                                    {@code -1..getParticipantsInInitiativeOrder().size() - 1}
     */
    public void restoreTurnCursor(final int round, final int index) {
        if (round < 0 || index < -1 || index >= activeEntries.size()) {
            throw new IllegalOperationException(INVALID_TURN_CURSOR);
        }
        this.currentRound = round;
        this.currentIndex = index;
    }

    /**
     * Removes characterSheet from this Scene entirely — the counterpart to {@link #addParticipant},
     * for a participant that dies or otherwise leaves partway through. Searches the rotation and
     * the pending set alike, matching on {@link CombatantSheet#getId()} the same way {@link
     * #getAllies}/{@link #groupOf} already do rather than on instance identity.
     *
     * <p>Removing from the rotation keeps the cursor pointing at the same CombatantSheet it
     * pointed at before: an entry at or before {@link #getCurrentIndex()} shifts everything after
     * it down one, so the cursor moves down with it. Removing whoever's Turn it currently *is*
     * therefore leaves the cursor on the participant just before them, so the next {@link #next()}
     * call lands on whoever would have followed — the departing participant's Turn simply ends
     * where it stood, and nobody is skipped. Emptying the rotation resets the cursor to {@code -1}.
     *
     * <p>Any {@link Blessing} this Scene granted characterSheet is revoked as part of the removal
     * (the same {@link CombatantSheet#removeEffect} call {@link #applyInitiativeBlessings} would
     * have made later), so {@link #grantedBlessings} never keeps tracking a sheet that is no
     * longer here — which would otherwise leave {@link #isGroupBlessed} answering for a group
     * whose only tracked member has left.
     * @return whether characterSheet was actually in this Scene
     */
    public boolean removeParticipant(final CombatantSheet characterSheet) {
        int activeIndex = indexOf(activeEntries, characterSheet);
        boolean removed = false;

        if (activeIndex >= 0) {
            activeEntries.remove(activeIndex);
            if (activeEntries.isEmpty()) {
                currentIndex = -1;
            } else if (activeIndex <= currentIndex) {
                currentIndex--;
            }
            removed = true;
        } else {
            int pendingIndex = indexOf(pendingEntries, characterSheet);
            if (pendingIndex >= 0) {
                pendingEntries.remove(pendingIndex);
                removed = true;
            }
        }

        if (removed) {
            grantedBlessings.entrySet().removeIf(entry -> {
                if (!entry.getKey().getId().equals(characterSheet.getId())) {
                    return false;
                }
                entry.getValue().forEach(entry.getKey()::removeEffect);
                return true;
            });
        }
        return removed;
    }

    /** Position of characterSheet in entries by {@link CombatantSheet#getId()}, or {@code -1}. */
    private int indexOf(final List<InitiativeEntry> entries, final CombatantSheet characterSheet) {
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).getCombatantSheet().getId().equals(characterSheet.getId())) {
                return i;
            }
        }
        return -1;
    }

    /**
     * The Round-boundary bookkeeping {@link #next()} runs every time it wraps back to the top:
     * merges in any {@link #pendingEntries} added mid-Round, then re-sorts {@link
     * #activeEntries} by every entry's current {@link InitiativeEntry#getEffectiveInitiativeValue()}
     * — not just each entry's fixed {@link InitiativeEntry#getInitiativeValue()} — so a
     * Round-scoped Iniciativa bonus granted or expired since the last Round boundary is
     * reflected in the turn order from this Round onward. {@code List#sort} is stable, so ties
     * (including a newly-merged pending entry tying with an existing one) keep whatever
     * relative order they already had, the same tie behavior {@link #insertSorted} preserves.
     */
    private void startNewRound() {
        activeEntries.addAll(pendingEntries);
        pendingEntries.clear();
        activeEntries.sort(Comparator.comparingInt(InitiativeEntry::getEffectiveInitiativeValue).reversed());
    }

    /** Inserts before the first entry with a strictly lower value, keeping ties in insertion order. */
    private void insertSorted(final List<InitiativeEntry> sortedEntries, final InitiativeEntry entry) {
        int insertionIndex = sortedEntries.size();
        for (int i = 0; i < sortedEntries.size(); i++) {
            if (sortedEntries.get(i).getEffectiveInitiativeValue() < entry.getEffectiveInitiativeValue()) {
                insertionIndex = i;
                break;
            }
        }
        sortedEntries.add(insertionIndex, entry);
    }
}
