package org.aventyrs.core.sheet;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.effect.CriticalEffectType;
import org.aventyrs.core.item.Item;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.rest.RestType;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Every {@link CombatantSheet} behaviour, implemented once — the resource pools, the temporary
 * Ego points, the {@link TemporaryEffect} list and the Turn lifecycle. {@link CharacterSheet}
 * and {@code org.aventyrs.core.monster.MonsterSheet} each extend this and add only their own
 * half (a player's experience and Fama; a monster's stat-block numbers), so none of the logic
 * below is duplicated between them.
 *
 * <p>An interface can't hold the state this needs, which is why the shared half is a class
 * rather than a pile of {@code default} methods. See {@link CombatantSheet} for what the split
 * is <i>for</i>.
 */
@Getter
public abstract class AbstractCombatantSheet implements CombatantSheet {

    /**
     * A unique, stable identifier for this specific sheet instance — distinct from {@link
     * Character#getId()}, since the same Character could in principle back more than one sheet.
     * {@link org.aventyrs.core.scene.Scene} keys its participants by this, not by
     * object-reference equality. Auto-generated; not {@code final}, so a subclass reconstructing
     * a sheet from persisted state (e.g. a DTO) can preserve an identity that already exists
     * rather than re-minting one — see {@link #restoreId}. There is deliberately no public
     * setter.
     */
    private UUID id = UUID.randomUUID();

    @NonNull
    private final Character character;

    private int shieldPoints = 0;

    @Getter(AccessLevel.NONE)
    private final ResourcePool hitPoints = new ResourcePool();

    @Getter(AccessLevel.NONE)
    private final ResourcePool magicPoints = new ResourcePool();

    @Getter(AccessLevel.NONE)
    private final ResourcePool determinationPoints = new ResourcePool();

    @Getter(AccessLevel.NONE)
    private final Map<EgoDomain, TemporaryPointPool> temporaryEgoPoints = newTemporaryEgoPointsPools();

    /**
     * Every {@link TemporaryEffect} (a {@link TemporaryBonus}, a {@link Bleeding}, …) this sheet
     * is currently holding — one shared list, since they all count down in Rodadas the same way
     * and must be ticked together (see {@link #tickTemporaryEffects()}) rather than risking a
     * double-decrement from two separate tick methods walking the same kind of state.
     */
    @Getter(AccessLevel.NONE)
    private final List<TemporaryEffect> temporaryEffects = new ArrayList<>();

    /**
     * The Itens this sheet's {@link #getCharacter()} owns but currently isn't wearing/wielding —
     * where an {@link Item} lands the moment it's bought (or otherwise acquired), via {@link
     * #addToInventory(Item)}. Lives here rather than on {@code Character} because it's per-sheet
     * acquired state, not a fixed trait of the Character itself (the same reason a player's
     * experience lives on {@link CharacterSheet}), and because a future PE (Pontos de
     * Equipamento) economy funding these purchases belongs alongside it — unlike {@code
     * Character#equipment}, which is a fixed trait of the Character (what they're currently
     * wearing/wielding) regardless of which sheet is in play.
     *
     * <p>On this shared class rather than {@link CharacterSheet} because loot is not a
     * player-only concept: a monster carrying a weapon it can drop is exactly this list.
     *
     * <p>Same catalog-entry-not-owned-copy caveat as {@code Character#equipment}: these are
     * {@link Item} constants, not per-copy instances, so holding the same constant twice
     * genuinely means owning two of them. Space is unlimited — no Carga/capacity concept exists
     * in this core, so nothing here ever rejects an addition.
     *
     * <p>No PE economy exists yet (see {@link Item}'s own javadoc), so "buying" isn't a validated
     * transaction here — a caller resolves whatever price-paying happens elsewhere and calls
     * {@link #addToInventory(Item)} once it has.
     */
    private final List<Item> inventory = new ArrayList<>();

    /** Temporary Ego points owed back at this sheet's next qualifying Rest. */
    @Getter(AccessLevel.NONE)
    private final List<PendingEgoRecovery> pendingEgoRecoveries = new ArrayList<>();

    /** Every {@link AttributeDomain} that has already governed a Perícia roll this Turn. */
    @Getter(AccessLevel.NONE)
    private final Set<AttributeDomain> attributeDomainsRolledThisTurn = EnumSet.noneOf(AttributeDomain.class);

    protected AbstractCombatantSheet(@NonNull final Character character) {
        this.character = character;
    }

    private static Map<EgoDomain, TemporaryPointPool> newTemporaryEgoPointsPools() {
        Map<EgoDomain, TemporaryPointPool> pools = new EnumMap<>(EgoDomain.class);
        for (EgoDomain domain : EgoDomain.values()) {
            pools.put(domain, new TemporaryPointPool());
        }
        return pools;
    }

    /**
     * Overwrites the auto-generated {@link #getId()} with an already-known one — for a subclass
     * factory reconstructing a sheet from persisted state, where the identity already exists.
     * Deliberately {@code protected}: this is a construction-time concern, not a setter.
     */
    protected void restoreId(@NonNull final UUID id) {
        this.id = id;
    }

    @Override
    public int getDamageTaken() {
        return hitPoints.getSpent();
    }

    @Override
    public int getManaSpent() {
        return magicPoints.getSpent();
    }

    @Override
    public int getDeterminationSpent() {
        return determinationPoints.getSpent();
    }

    /**
     * Applies damage, consuming any Shield points first.
     * @return int total damage accumulated so far
     */
    @Override
    public int applyDamage(final int amount) {
        int remaining = amount;
        if (shieldPoints > 0) {
            int absorbed = Math.min(shieldPoints, remaining);
            shieldPoints -= absorbed;
            remaining -= absorbed;
        }
        return hitPoints.spend(remaining);
    }

    /**
     * Applies curse damage, which drains life directly and bypasses Shield points.
     * @return int total damage accumulated so far
     */
    @Override
    public int applyCurseDamage(final int amount) {
        return hitPoints.spend(amount);
    }

    /**
     * Heals accumulated damage — the same recovery a Rest applies to PV. Also interrupts every
     * active {@link Bleeding} (Sangramento's own "Efeitos de cura interrompem a perda de PV por
     * rodada"); the immediate PV already lost stays lost, only the ongoing drain stops.
     * @return int remaining damage accumulated
     */
    @Override
    public int heal(final int amount) {
        if (amount > 0) {
            temporaryEffects.removeIf(effect -> effect instanceof Bleeding);
        }
        return hitPoints.recover(amount);
    }

    /**
     * Grants Shield points, absorbed before damage reaches Hit Points.
     * @return int total Shield points available
     */
    @Override
    public int addShield(final int amount) {
        return shieldPoints += amount;
    }

    @Override
    public int spendMagicPoints(final int amount) {
        return magicPoints.spend(amount);
    }

    /**
     * Recovers spent Magic Points — the same recovery a Rest applies to PM. Also interrupts every
     * active {@link ManaDrain}, the {@link #heal} counterpart for ManaPurge's own clause.
     */
    @Override
    public int recoverMagicPoints(final int amount) {
        if (amount > 0) {
            temporaryEffects.removeIf(effect -> effect instanceof ManaDrain);
        }
        return magicPoints.recover(amount);
    }

    @Override
    public int spendDeterminationPoints(final int amount) {
        return determinationPoints.spend(amount);
    }

    @Override
    public int recoverDeterminationPoints(final int amount) {
        return determinationPoints.recover(amount);
    }

    @Override
    public int getTemporaryEgoPoints(final EgoDomain domain) {
        return temporaryEgoPoints.get(domain).getAmount();
    }

    @Override
    public int gainTemporaryEgoPoints(final EgoDomain domain, final int amount) {
        return temporaryEgoPoints.get(domain).gain(amount);
    }

    /**
     * Gains a non-cumulative temporary point in domain, attributed to source — repeat gains from
     * that <i>same</i> source don't stack past what it already granted, while a different
     * source's gain still adds normally on top. See {@link TemporaryPointPool#gainNonCumulative}.
     */
    @Override
    public int gainNonCumulativeTemporaryEgoPoints(final EgoDomain domain, final Object source, final int amount) {
        return temporaryEgoPoints.get(domain).gainNonCumulative(source, amount);
    }

    @Override
    public int spendTemporaryEgoPoints(final EgoDomain domain, final int amount) {
        return temporaryEgoPoints.get(domain).spend(amount);
    }

    /**
     * Registers a {@link PendingEgoRecovery} — e.g. {@code org.aventyrs.core.effect.Primor}'s
     * promise that the temporary Ego points it just spent come back at the next qualifying Rest.
     * Doesn't spend anything itself; the caller spends first, then registers the return.
     */
    @Override
    public void owePendingEgoRecovery(final PendingEgoRecovery recovery) {
        pendingEgoRecoveries.add(recovery);
    }

    /**
     * Resolves every {@link PendingEgoRecovery} satisfied by a Rest of restType's tier, granting
     * each one's owed points back, and discards them.
     */
    @Override
    public void applyPendingEgoRecoveries(final RestType restType) {
        pendingEgoRecoveries.stream()
                .filter(recovery -> recovery.isSatisfiedBy(restType))
                .forEach(recovery -> gainTemporaryEgoPoints(recovery.getDomain(), recovery.getValue()));
        pendingEgoRecoveries.removeIf(recovery -> recovery.isSatisfiedBy(restType));
    }

    /**
     * Adds item to this sheet's {@link #getInventory()} — a plain mutator: it validates nothing
     * (no Preço/PE spend, no capacity check, since neither exists yet).
     */
    @Override
    public void addToInventory(final Item item) {
        inventory.add(item);
    }

    /**
     * Removes one occurrence of item from {@link #getInventory()}, returning whether anything was
     * actually removed. The mirror of {@link #addToInventory(Item)}, and equally unvalidating.
     */
    @Override
    public boolean removeFromInventory(final Item item) {
        return inventory.remove(item);
    }

    @Override
    public InteractionResult receiveInteraction(final Interaction<CombatantSheet> interaction) {
        return interaction.applyTo(this);
    }

    /**
     * Registers a {@link TemporaryEffect}. Doesn't apply any immediate effect itself — e.g.
     * Sangramento's own immediate PV loss goes through {@link #applyDamage} before this is called
     * for its ongoing half.
     *
     * <p>When {@code effect.isCumulative()} is false (e.g. {@link Withering}), any existing
     * instance of the same concrete type is removed first — reapplying replaces it rather than
     * stacking a second one alongside.
     */
    @Override
    public void applyEffect(final TemporaryEffect effect) {
        if (!effect.isCumulative()) {
            temporaryEffects.removeIf(existing -> existing.getClass() == effect.getClass());
        }
        temporaryEffects.add(effect);
    }

    /**
     * Removes exactly this {@link TemporaryEffect} instance — reference-based (neither {@code
     * TemporaryEffect} nor {@link TemporaryBonus} overrides {@code equals()}) rather than matching
     * by type/value, so a caller revoking precisely what it granted doesn't disturb an unrelated
     * effect of the same {@link ModifierType} from another source (see {@code
     * Scene#applyInitiativeBlessings}). A no-op if effect isn't currently held.
     */
    @Override
    public void removeEffect(final TemporaryEffect effect) {
        temporaryEffects.remove(effect);
    }

    /**
     * Grants a {@link TemporaryBonus} of type. The granting Character isn't tracked (nothing
     * about this mechanism needs to know who granted it); locate targets via {@code
     * Scene#getAllies}.
     * @return int the total of type's active temporary bonuses after granting this one
     */
    @Override
    public int grantTemporaryBonus(final ModifierType type, final int value, final int rounds) {
        applyEffect(new TemporaryBonus(type, value, rounds));
        return getTemporaryBonus(type);
    }

    /** The sum of every currently-active (non-expired) {@link TemporaryBonus} of type. */
    @Override
    public int getTemporaryBonus(final ModifierType type) {
        return temporaryEffects.stream()
                .filter(effect -> effect instanceof TemporaryBonus)
                .map(effect -> (TemporaryBonus) effect)
                .filter(bonus -> !bonus.isExpired())
                .filter(bonus -> bonus.getType() == type)
                .mapToInt(TemporaryBonus::getValue)
                .sum();
    }

    /**
     * The sum of every currently-active {@link LifeSteal} effect's value — the {@link LifeSteal}
     * counterpart to {@link #getTemporaryBonus}, queried directly since {@link LifeSteal} isn't a
     * {@link ModifierType}. {@code LifeStealService#getTotalLifeSteal} is the intended caller.
     */
    @Override
    public int getTotalLifeSteal() {
        return temporaryEffects.stream()
                .filter(effect -> effect instanceof LifeSteal)
                .map(effect -> (LifeSteal) effect)
                .filter(effect -> !effect.isExpired())
                .mapToInt(LifeSteal::getValue)
                .sum();
    }

    /**
     * Advances every held {@link TemporaryEffect} by one Rodada: applies each one's per-Rodada
     * side effect, then counts them all down and discards any that expire. A single method over
     * the shared list rather than one per effect kind — ticking each kind separately would
     * decrement the other kinds' Rodadas too — dispatching through the polymorphic {@code
     * applyRoundEffect} hook so a future subclass needs no change here.
     */
    @Override
    public void tickTemporaryEffects() {
        temporaryEffects.forEach(effect -> effect.applyRoundEffect(this));
        temporaryEffects.forEach(TemporaryEffect::tick);
        temporaryEffects.removeIf(TemporaryEffect::isExpired);
    }

    /**
     * None, unless a subclass says otherwise — a combatant is vulnerable to every Efeito Crítico
     * by default, and only an authored anatomy clause changes that. {@code MonsterSheet}
     * overrides this; {@code CharacterSheet} deliberately does not (no player-facing trait grants
     * one yet — see {@code ProfissaoCompetencyAbility}'s still-unbuilt Resistência a Críticos).
     */
    @Override
    public Set<CriticalEffectType> getCriticalEffectImmunities() {
        return Set.of();
    }

    /**
     * Ends this combatant's Turn. Currently just advances its {@link TemporaryEffect}s by one
     * Rodada — each participant has one Turn per Rodada, so ticking once per Turn-end is exactly
     * "once per Rodada" from this sheet's perspective. Expected to grow as more per-Turn
     * bookkeeping is added, which is why it's {@code void} rather than reporting any one piece.
     */
    @Override
    public void finishTurn() {
        tickTemporaryEffects();
    }

    /**
     * Begins this combatant's Turn, the mirror of {@link #finishTurn()}. turnNumber is 0-based,
     * the same convention {@code ActionPointsService}/{@code MovementService} use.
     *
     * <p>Clears {@link #attributeDomainsRolledThisTurn} — see {@link #consumeFirstRollThisTurn},
     * whose first real consumer is {@code DexterityAbility#PRECISAO}'s "primeira rolagem... em
     * cada um de seus Turnos". Nothing else triggers "no início do seu turno" yet
     * ({@link Bleeding}/{@link ManaDrain}/{@link Withering} all apply at Turn-<em>end</em>), so
     * turnNumber stays unused for now, still in hand for whatever plugs in next.
     */
    @Override
    public void startTurn(final int turnNumber) {
        attributeDomainsRolledThisTurn.clear();
    }

    /**
     * Whether rolledDomain hasn't yet governed a Perícia roll this Turn — and, if so, marks it as
     * having happened now, in the same call. {@link Set#add}'s return value makes this an atomic
     * query-and-consume, exactly what a "first roll of the Turn" check/claim needs. Reset by
     * {@link #startTurn}; without a live {@code Scene} ever calling that, the set simply starts
     * empty, so the first roll for any domain still correctly reads as "first".
     *
     * <p>Called unconditionally by {@code AbstractSkillInteraction} for every actual roll,
     * regardless of whether the roller holds any ability that cares — this tracks an objective
     * fact about the Turn, independent of who reacts to it.
     */
    @Override
    public boolean consumeFirstRollThisTurn(final AttributeDomain rolledDomain) {
        return attributeDomainsRolledThisTurn.add(rolledDomain);
    }
}
