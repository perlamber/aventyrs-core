package org.aventyrs.core.sheet;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.effect.CriticalEffectType;
import org.aventyrs.core.item.Item;
import org.aventyrs.core.item.Weapon;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.rest.RestType;
import org.aventyrs.core.scene.SceneContext;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Anything that can take part in a combat exchange — everything a {@link CharacterSheet} and a
 * {@code org.aventyrs.core.monster.MonsterSheet} do identically.
 *
 * <h2>Why this split exists</h2>
 *
 * A foe is genuinely a different thing from a player character: no {@link Player}, no experience
 * to spend, no Fama, and stat-block numbers where a player has dice. But from the point of view
 * of damage, Efeitos, Defesas, initiative and the Turn lifecycle, the two are interchangeable —
 * a Sangramento doesn't care who it's bleeding. This interface is exactly that interchangeable
 * half; {@link CharacterSheet} keeps the player-only half to itself.
 *
 * <p><b>The split is what keeps monsters out of the progression system</b>, and it does it
 * through the type system rather than a runtime check. Experience lives on {@code
 * CharacterSheet}, so the four services that spend it — {@code
 * CharacterAttributeService#upgradeBase}, {@code SkillGraduationService#upgradeGraduation},
 * {@code FeatService#grantFeat}, {@code TitleAbilityService#grantTitleAbility} — keep taking the
 * concrete {@code CharacterSheet}, and a monster simply cannot be passed to them. That matters
 * because a monster's Attributes and Graduações are deliberately uncapped: nothing stops
 * {@code AttributeValue.builder().base(9)}, so the thing worth preventing was never "exceeding
 * the cap", it was "levelling up like a character". No flag, no {@code isMonster()}, no
 * validation — it doesn't compile.
 *
 * <p>Ego points are on this side of the line even though they read as player-facing:
 * {@code org.aventyrs.core.effect.Primor} applies to a <i>target</i>, so leaving them off would
 * break Primor the moment it lands on a foe. {@link EgoDomain}'s own javadoc says "a creature",
 * not "a player".
 *
 * <p>{@link AbstractCombatantSheet} implements every method here once; both sheet kinds extend
 * it rather than reimplementing the resource pools and effect list.
 */
public interface CombatantSheet extends Interactable<CombatantSheet> {

    /** A stable identity for this combatant — what {@code Scene} keys its participants by. */
    UUID getId();

    /** The Attributes, Perícias, abilities and equipment behind this combatant. */
    Character getCharacter();

    // --- Hit Points and shields -----------------------------------------------------------

    int getDamageTaken();

    /** Applies damage, consuming any Shield points first. */
    int applyDamage(int amount);

    /** Applies curse damage, which drains life directly and bypasses Shield points. */
    int applyCurseDamage(int amount);

    /** Heals accumulated damage, interrupting any ongoing {@link Bleeding}. */
    int heal(int amount);

    int getShieldPoints();

    int addShield(int amount);

    // --- Mana and Determinação ------------------------------------------------------------

    int getManaSpent();

    int spendMagicPoints(int amount);

    /** Recovers spent Magic Points, interrupting any ongoing {@link ManaDrain}. */
    int recoverMagicPoints(int amount);

    int getDeterminationSpent();

    int spendDeterminationPoints(int amount);

    int recoverDeterminationPoints(int amount);

    // --- Inventory ---------------------------------------------------------------------------

    /** Itens owned but not currently worn or wielded — loot included. */
    List<Item> getInventory();

    void addToInventory(Item item);

    boolean removeFromInventory(Item item);

    // --- Ego points -------------------------------------------------------------------------
    // Two spendable pools per EgoDomain, permanent and temporary; see EgoPointPool for the model
    // and for why spending a permanent point costs twice over.

    /** Permanent points not yet spent — the Ego stat itself is their maximum. */
    int getPermanentEgoPoints(EgoDomain domain);

    /** Temporary points not yet spent, under the live ceiling. */
    int getTemporaryEgoPoints(EgoDomain domain);

    /** How many temporary points this domain may hold right now. */
    int getMaxTemporaryEgoPoints(EgoDomain domain);

    /** Everything this domain can still pay with, from either pool. */
    int getAvailableEgoPoints(EgoDomain domain);

    /** Spends from the pool the caller names, reporting what actually left it. */
    EgoPointSpend spendEgoPoints(EgoDomain domain, EgoPointType type, int amount);

    /** Restores previously-spent temporary points, bounded by the ceiling. */
    int recoverTemporaryEgoPoints(EgoDomain domain, int amount);

    /** Raises this domain's temporary ceiling, non-cumulatively per source. */
    int grantTemporaryEgoPointBonus(EgoDomain domain, Object source, int amount);

    void owePendingEgoRecovery(PendingEgoRecovery recovery);

    void applyPendingEgoRecoveries(RestType restType);

    // --- Temporary effects ------------------------------------------------------------------

    void applyEffect(TemporaryEffect effect);

    void removeEffect(TemporaryEffect effect);

    int grantTemporaryBonus(ModifierType type, int value, int rounds);

    int getTemporaryBonus(ModifierType type);

    // --- Condições / Malefícios ---------------------------------------------------------------

    /**
     * Puts this combatant under condition, replacing any held {@link Condition} of the same
     * {@link ConditionType} (refreshing its duration) rather than stacking a second — see {@link
     * Condition}'s own javadoc for why a condition is a state, not a stacking bonus.
     */
    void applyCondition(Condition condition);

    /** Lifts conditionType, along with anything it was implying. A no-op if not held. */
    void removeCondition(ConditionType conditionType);

    /**
     * Every {@link ConditionType} currently in force, including ones only <i>implied</i> by
     * another (Caído confers Desprevenido), resolved transitively. An implication scoped to a
     * {@link org.aventyrs.core.scene.Range} counts only while that proximity holds, which is why
     * this takes the sceneContext — Assustado confers Desprevenido, but only while adjacent to
     * the origin of the fear.
     */
    Set<ConditionType> getActiveConditions(SceneContext sceneContext);

    /** Whether conditionType is in force, directly or by implication. */
    boolean hasCondition(ConditionType conditionType, SceneContext sceneContext);

    /**
     * The summed numeric malus every active condition contributes toward modifierType — the
     * {@link Condition} counterpart to {@link #getTemporaryBonus}, queried separately because a
     * condition's effect can be proximity-scoped and so needs the Scene to resolve. A service
     * reading a {@link ModifierType} sums both.
     */
    int getConditionBonus(ModifierType modifierType, SceneContext sceneContext);

    /**
     * Takes weapon in hand and records that it happened <b>this Turn</b> — the timed form of
     * {@code Character#drawWeapon(Weapon)}, which knows nothing about Turns. What {@code
     * AssassinoFeat#SAQUE_RAPIDO}'s "a primeira rolagem de Perícia de Ataque que realizar neste
     * mesmo turno" is measured against.
     *
     * @return whether the weapon was actually drawn (false if not carried, or already in hand)
     */
    boolean drawWeapon(Weapon weapon);

    /**
     * Whether this combatant has drawn a weapon since their Turn began. Reset by {@link
     * #startTurn(int)}, like every other per-Turn marker; with no live {@code Scene} calling
     * that, it simply reads false until something is drawn.
     */
    boolean hasDrawnWeaponThisTurn();

    /**
     * Whether this combatant has drawn a weapon at any point since this Cena began. Reset by
     * {@link #startNewScene()} (which {@code Scene#addParticipant} calls); {@code
     * AssassinoFeat#SAQUE_RELAMPAGO}'s "imediatamente após sacar sua primeira arma... na Cena de
     * Combate" rider is measured against it. With no live {@code Scene} it reads false until a
     * weapon is drawn and then stays true.
     */
    boolean hasDrawnWeaponThisScene();

    /**
     * Knocks weapon out of this combatant's hands — the effect that inflicts {@link
     * ConditionType#DESARMADO}. Unequips it and, if nothing else armed remains, applies the
     * condition.
     *
     * @return the weapon actually dropped, or empty when it was not wielded or {@link
     * Weapon#isDisarmable()} refuses ("Não pode ser desarmado")
     *
     * <p><b>The dropped weapon is handed back, not put anywhere.</b> A disarmed weapon falls on
     * the ground, and this core models no ground — so the caller decides where it lands (an
     * enemy's hand, the floor, {@link #addToInventory} if it is merely stowed). Returning it is
     * that handoff; losing track of it would be the alternative.
     */
    java.util.Optional<Weapon> disarm(Weapon weapon);

    /**
     * Puts weapon back in this combatant's hands, lifting {@link ConditionType#DESARMADO}. The
     * mirror of {@link #disarm(Weapon)} — being Desarmado lasts until you are armed again, not a
     * fixed number of Rodadas, which is why the condition it applies is open-ended.
     *
     * @return whether the weapon was actually taken up; {@code false} when a held condition
     * forbids it ({@link ConditionType#preventsArming()} — Devorado, where nothing you dropped
     * is reachable from inside a creature), in which case neither the equipment nor the condition
     * changes
     */
    boolean rearm(Weapon weapon);

    /**
     * Whether this combatant may currently attack with weapon — {@code null} meaning an Ataque
     * Desarmado, which is always allowed. False only while a held condition restricts them to
     * light weapons ({@link ConditionType#restrictsAttacksToLightWeapons()} — Devorado, where a
     * greatsword cannot be brought to bear inside a creature but a dagger still can).
     *
     * <p>A question, not a gate: nothing in this core refuses an attack made with a weapon this
     * returns {@code false} for, because there is no validation point between choosing an attack
     * and resolving one. A caller deciding which attacks to present asks this.
     */
    boolean canAttackWith(Weapon weapon);

    /**
     * The flat dano-roll bonus this combatant's conditions grant to <b>whoever attacks them</b> —
     * Flanqueado's "Atacar um personagem Flanqueado garante Vantagem na rolagem de Dano". Read
     * off the <em>target</em>'s sheet by {@code AbstractSkillInteraction}, the mirror of {@link
     * #getConditionBonus}, which is what the holder themselves suffers.
     */
    int getAttackerDamageBonusFromConditions(SceneContext sceneContext);

    /**
     * Whether a held condition forbids moving at all — Agarrado/Imobilizado's "não pode realizar
     * movimentos". A prohibition, deliberately not a large negative {@link ModifierType#MOVEMENT}:
     * {@code MovementService} reports 0 rather than arithmetic that happens to floor there.
     */
    boolean isMovementPrevented(SceneContext sceneContext);

    /**
     * Whether a held condition forbids recovering Pontos de Vida — Feridas Dolorosas' "não pode
     * ser curado e nem regenerar". Takes no Scene: no condition scopes this by proximity.
     */
    boolean isHealingPrevented();

    /** Whether a held condition forbids activating Habilidades de Aventyr/de Monstro — Silêncio. */
    boolean isAbilityActivationPrevented(SceneContext sceneContext);

    /** Whether a held condition forbids Conjurar Magias — Silêncio. */
    boolean isSpellCastingPrevented(SceneContext sceneContext);

    int getTotalLifeSteal();

    void tickTemporaryEffects();

    /**
     * The Efeitos Críticos this combatant's anatomy simply shrugs off — {@code
     * org.aventyrs.core.effect.CriticalEffect#applicableTo} drops each of these from an attack's
     * chain before any of it is applied. Empty for anything without a stat block clause saying
     * otherwise, which is every player character today.
     *
     * <p>On the shared half rather than on {@code MonsterSheet} for the same reason temporary
     * Ego points are: an immunity is a property of <i>a target</i>, and both kinds of sheet can
     * be one. A player character resisting an Efeito Crítico is a shape this ruleset already
     * gestures at (see {@code ProfissaoCompetencyAbility}'s Resistência a Críticos), so the hook
     * is not monster-only even though only a monster overrides it right now.
     */
    Set<CriticalEffectType> getCriticalEffectImmunities();

    // --- Turn lifecycle -----------------------------------------------------------------------

    /**
     * Begins this combatant's Turn. turnNumber is 0-based. Also marks where this Turn's actions
     * begin in {@link #getActionsThisRound()}, so {@link #isFirstRollOfTurnFor} can answer a
     * per-Turn question against the per-Rodada log.
     */
    void startTurn(int turnNumber);

    /** Ends this combatant's Turn, advancing its {@link TemporaryEffect}s by one Rodada. */
    void finishTurn();

    /**
     * Begins a new Rodada for this combatant: clears {@link #getActionsThisRound()} and resets
     * the per-Turn marker {@link #startTurn(int)} sets. Called by {@code
     * org.aventyrs.core.scene.Scene#next()} on every active participant at the Rodada wrap,
     * <em>before</em> {@code startTurn} on whoever acts first. With no live {@code Scene} ever
     * calling it, the log simply starts empty and the API must call this at its own Rodada
     * boundary — the same fallback {@link #consumeMovementThisRound()} documents.
     */
    void startNewRound();

    /**
     * Begins a new Cena for this combatant: clears {@link #getActionsThisCena()} (and, with it,
     * {@link #getActionsThisRound()}) and {@link #hasDrawnWeaponThisScene()}. Called by {@code
     * Scene#addParticipant} when this sheet joins a Scene; the API calls it directly otherwise.
     * A "primeira ... na Cena" clause needs a boundary the per-Rodada log cannot give.
     */
    void startNewScene();

    /**
     * Appends an action this combatant took this Rodada — see {@link CombatantAction}. The API
     * calls this <b>explicitly</b> after resolving a roll (or an {@code AttackDelivery}/{@code
     * AttackReceiver} exchange); it is never a side effect of {@code
     * AbstractSkillInteraction#applyTo}. The action also lands in {@link #getActionsThisCena()}.
     */
    void recordAction(CombatantAction action);

    /** Every action recorded since this Rodada began — an unmodifiable view, in order. */
    List<CombatantAction> getActionsThisRound();

    /**
     * Every action recorded since this Cena began — an unmodifiable view, in order. Unlike
     * {@link #getActionsThisRound()} this is not cleared at the Rodada wrap, only by {@link
     * #startNewScene()}; a "primeira magia conjurada na Cena" / "primeiro ataque de cada cena"
     * clause reads this.
     */
    List<CombatantAction> getActionsThisCena();

    /**
     * Whether no action recorded during this combatant's current Turn was governed by domain —
     * the non-mutating replacement for the old {@code consumeFirstRollThisTurn}. "This Turn" is
     * the slice of {@link #getActionsThisRound()} after the marker {@link #startTurn(int)} set;
     * the "mark it happened" step is now the API's {@link #recordAction} call. Gates {@code
     * AttributeAbility#resolveFirstRollOfTurnBonus} ({@code DexterityAbility#PRECISAO}).
     */
    boolean isFirstRollOfTurnFor(AttributeDomain domain);

    /**
     * Whether no <b>Perícia de Ataque</b> roll has been made yet this Turn — the attack-scoped
     * sibling of {@link #isFirstRollOfTurnFor(AttributeDomain)}, sliced from the same per-Rodada
     * log at the same {@link #startTurn(int)} marker.
     *
     * <p>A non-mutating read, and the roll being resolved is not yet in the log (the API records
     * it afterwards), so asking during resolution correctly answers "is <i>this</i> the first".
     */
    boolean isFirstAttackRollOfTurn();

    /**
     * How many movements this combatant has already made this Rodada — 0 before the first one,
     * so it doubles as the 0-based index of whichever movement comes next. Read, never consumed:
     * a caller previewing "how far would my next movement go" asks this; one actually moving
     * calls {@link #consumeMovementThisRound()} instead.
     */
    int getMovementsTakenThisRound();

    /**
     * Records a movement as taken, returning its own 0-based index within this Rodada — 0 for
     * the first. The mutating counterpart of {@link #getMovementsTakenThisRound()}: a clause
     * scoped to "seu primeiro movimento em cada Rodada" needs to claim that position in the act
     * of asking for it, or two callers could both believe they were first. (Roll-actions use the
     * separate {@link #recordAction}/{@link #getActionsThisRound()} log instead.)
     */
    int consumeMovementThisRound();
}
