package org.aventyrs.core.sheet;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.effect.CriticalEffectType;
import org.aventyrs.core.item.Item;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.rest.RestType;

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

    // --- Temporary Ego points ---------------------------------------------------------------

    int getTemporaryEgoPoints(EgoDomain domain);

    int gainTemporaryEgoPoints(EgoDomain domain, int amount);

    int gainNonCumulativeTemporaryEgoPoints(EgoDomain domain, Object source, int amount);

    int spendTemporaryEgoPoints(EgoDomain domain, int amount);

    void owePendingEgoRecovery(PendingEgoRecovery recovery);

    void applyPendingEgoRecoveries(RestType restType);

    // --- Temporary effects ------------------------------------------------------------------

    void applyEffect(TemporaryEffect effect);

    void removeEffect(TemporaryEffect effect);

    int grantTemporaryBonus(ModifierType type, int value, int rounds);

    int getTemporaryBonus(ModifierType type);

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

    /** Begins this combatant's Turn. turnNumber is 0-based. */
    void startTurn(int turnNumber);

    /** Ends this combatant's Turn, advancing its {@link TemporaryEffect}s by one Rodada. */
    void finishTurn();

    /**
     * Whether rolledDomain hasn't yet governed a Perícia roll this Turn — and, if so, marks it
     * as having happened now, in the same call.
     */
    boolean consumeFirstRollThisTurn(AttributeDomain rolledDomain);
}
