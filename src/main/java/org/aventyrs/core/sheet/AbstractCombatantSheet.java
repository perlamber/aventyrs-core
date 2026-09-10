package org.aventyrs.core.sheet;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import org.aventyrs.core.ability.AttributeAbility;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.ability.ActiveAbility;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.effect.CriticalEffectType;
import org.aventyrs.core.feat.Feat;
import org.aventyrs.core.item.Item;
import org.aventyrs.core.item.ItemWeightClass;
import org.aventyrs.core.item.Weapon;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.rest.RestType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.stream.Stream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.aventyrs.core.util.TranslatableMessages.NOT_ENOUGH_EQUIPMENT_POINTS;

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

    /**
     * Both spendable Ego point pools, per {@link EgoDomain} — see {@link EgoPointPool} for the
     * permanent/temporary model and why each is a spent-counter rather than a held balance.
     */
    @Getter(AccessLevel.NONE)
    private final Map<EgoDomain, EgoPointPool> egoPoints = newEgoPointPools();

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
     * experience lives on {@link CharacterSheet}), and because the PE (Pontos de Equipamento)
     * economy funding these purchases lives alongside it in {@link #equipmentPoints} — unlike
     * {@code Character#equipment}, which is a fixed trait of the Character (what they're currently
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
     * <p>Buying <em>is</em> a validated transaction now, but not here: {@code
     * org.aventyrs.core.character.services.ItemPurchaseService} debits {@link #equipmentPoints}
     * and calls {@link #addToInventory(Item)} with the forged copy. Every other way an item
     * arrives (loot, a GM grant, a self-forge) still just calls {@code addToInventory}.
     */
    private final List<Item> inventory = new ArrayList<>();

    /**
     * This sheet's Pontos de Equipamento balance — the budget {@code
     * org.aventyrs.core.character.services.ItemPurchaseService} spends to buy Equipamento, and
     * that {@code ResourcesAdvantage#BARGANHISTA} discounts. A plain held balance (contrast the
     * Ego pools' spent-counters), moved by {@link #grantEquipmentPoints(int)} /
     * {@link #spendEquipmentPoints(int)}, the same shape as {@link CharacterSheet}'s experience
     * wallet. On this shared class, next to {@link #inventory}, for the reason that field's
     * javadoc gives; a monster simply leaves it at 0.
     */
    private int equipmentPoints = 0;

    /** Temporary Ego points owed back at this sheet's next qualifying Rest. */
    @Getter(AccessLevel.NONE)
    private final List<PendingEgoRecovery> pendingEgoRecoveries = new ArrayList<>();

    /** Temporary Ego points owed at the start of this sheet's next Rodada — see {@link DelayedEgoGrant}. */
    @Getter(AccessLevel.NONE)
    private final List<DelayedEgoGrant> scheduledEgoGrants = new ArrayList<>();

    /**
     * Every once-per-game-session marker already claimed — see {@link #consumeOncePerSession}.
     * {@code transient} on purpose, and that is the whole session model: a session is this
     * sheet object's lifetime in the running client, so these markers must never travel into
     * persisted state and hand a reloaded sheet a session it has already used up.
     */
    @Getter(AccessLevel.NONE)
    private final transient Set<Object> consumedSessionMarkers = new HashSet<>();

    /**
     * Rodadas of Resfriamento still owed per {@link ActiveAbility} — see {@link
     * #startCooldown}/{@link #getRemainingCooldown}. Keyed by <b>identity</b>, matching how
     * {@code ActiveAbilityService#activate} recognises a held ability ({@code ==}, since {@code
     * Character#getActiveAbilities()} aggregates live and a Talento-supplied ability must return
     * a stable singleton). An {@code IdentityHashMap} says that outright rather than relying on
     * enum constants happening to have identity equality.
     */
    @Getter(AccessLevel.NONE)
    private final Map<ActiveAbility, Integer> cooldowns = new java.util.IdentityHashMap<>();

    /**
     * The alternate shape this combatant is currently in — {@code null} for their own, which is
     * the state every sheet starts and mostly stays in. See {@link FormType}.
     */
    @Getter(AccessLevel.NONE)
    private FormType currentForm;

    /**
     * Abilities that cannot be used again until a Descanso of the recorded tier — the other half
     * of Resfriamento, kept apart from {@link #cooldowns} because a Descanso is not a number of
     * Rodadas. Identity-keyed for the same reason.
     */
    @Getter(AccessLevel.NONE)
    private final Map<ActiveAbility, RestType> restCooldowns = new java.util.IdentityHashMap<>();

    /** Every roll-action taken since this Rodada began — see {@link #recordAction}. */
    @Getter(AccessLevel.NONE)
    private final List<CombatantAction> actionsThisRound = new ArrayList<>();

    /** Every roll-action taken since this Cena began — cleared only by {@link #startNewScene()}. */
    @Getter(AccessLevel.NONE)
    private final List<CombatantAction> actionsThisCena = new ArrayList<>();

    /** Index into {@link #actionsThisRound} where the current Turn's actions begin — set by {@link #startTurn}. */
    @Getter(AccessLevel.NONE)
    private int actionCountAtTurnStart = 0;

    /** Movements taken since this Rodada began — see {@link #consumeMovementThisRound()}. */
    private int movementsTakenThisRound = 0;

    /** Whether a weapon was drawn since this Turn began — see {@link #drawWeapon(Weapon)}. */
    private boolean drewWeaponThisTurn = false;

    /** Whether a weapon was drawn at any point since this Cena began — cleared by {@link #startNewScene()}. */
    private boolean drewWeaponThisScene = false;

    /** Whether {@link #startCombat()} has already fired this Cena — its idempotency guard, re-armed by {@link #startNewScene()}. */
    private boolean combatStarted = false;

    protected AbstractCombatantSheet(@NonNull final Character character) {
        this.character = character;
    }

    private static Map<EgoDomain, EgoPointPool> newEgoPointPools() {
        Map<EgoDomain, EgoPointPool> pools = new EnumMap<>(EgoDomain.class);
        for (EgoDomain domain : EgoDomain.values()) {
            pools.put(domain, new EgoPointPool());
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
        // Feridas Dolorosas: "não pode ser curado e nem regenerar pontos de vida". Refused
        // outright rather than reduced to 0 healing, so a Sangramento isn't cleared either.
        if (isHealingPrevented()) {
            return getDamageTaken();
        }
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

    /**
     * This domain's permanent maximum — the Ego stat itself, {@code base + variable}.
     *
     * <p>Read straight off {@link #getCharacter()} rather than through a service, unlike max Hit
     * Points: resolving <em>that</em> needs a {@code ModifierResolver} three-source
     * {@code @Modifier} scan, which is why {@code HitPointsService} owns it and why {@code
     * org.aventyrs.core.sheet} must not depend on {@code org.aventyrs.core.character.services}.
     * A permanent Ego maximum needs no scan at all — it is a two-field sum on {@code EgoValue},
     * over data this sheet already holds, and {@code org.aventyrs.core.character} is a legal
     * dependency here.
     *
     * <p><strong>Not to be confused with {@code InitiativeService#getTotalInitiative}</strong>,
     * which reads this same {@code EgoValue.getTotal()} but then adds a {@code
     * ModifierType.INITIATIVE} three-source sum, because Iniciativa doubles as a turn-order stat.
     * A {@code TemporaryBonus(INITIATIVE, +2, 2)} must never widen how many Iniciativa
     * <em>points</em> a combatant can spend. This asymmetry is the likeliest future misreading of
     * the two.
     */
    private int getPermanentEgoMax(final EgoDomain domain) {
        return character.getEgos().getEgo(domain).getTotal();
    }

    /**
     * The sum of every currently-active {@link TemporaryEgoPenalty} against domain — the {@link
     * TemporaryEgoPenalty} counterpart to {@link #getTemporaryBonus}, queried directly since an
     * Ego penalty deliberately isn't a {@link ModifierType} (see that class's own javadoc).
     * Private: {@link #getMaxTemporaryEgoPoints} already exposes the net effect, which is what a
     * consumer actually needs.
     */
    private int sumEgoPenalty(final EgoDomain domain) {
        return temporaryEffects.stream()
                .filter(effect -> effect instanceof TemporaryEgoPenalty)
                .map(effect -> (TemporaryEgoPenalty) effect)
                .filter(penalty -> !penalty.isExpired())
                .filter(penalty -> penalty.getDomain() == domain)
                .mapToInt(TemporaryEgoPenalty::getValue)
                .sum();
    }

    /**
     * Permanent points not yet spent in domain.
     *
     * <p>Note this sheet's {@link #getCharacter()} is {@code final}, while {@code
     * AttributeAbilityServiceImpl#grantAttributeAbility} applies a permanent Ego gain by
     * returning a <em>rebuilt</em> {@code Character}. So a permanent point earned after this
     * sheet was constructed is invisible to it until a new sheet is built from the new Character
     * — the same pre-existing ordering gap {@code MoralHerdadaAbility#applyStartingFama} sits on,
     * not something this pool introduces.
     */
    @Override
    public int getPermanentEgoPoints(final EgoDomain domain) {
        return egoPoints.get(domain).getPermanentRemaining(getPermanentEgoMax(domain));
    }

    /**
     * How many temporary points domain may hold right now — permanent points <em>remaining</em>
     * plus every granted bonus, minus any active {@link TemporaryEgoPenalty}. See {@link
     * EgoPointPool} for why this tracks permanent remaining rather than the permanent maximum.
     */
    @Override
    public int getMaxTemporaryEgoPoints(final EgoDomain domain) {
        return egoPoints.get(domain).getTemporaryCeiling(getPermanentEgoMax(domain), sumEgoPenalty(domain));
    }

    /**
     * Temporary points not yet spent in domain, under the live {@link
     * #getMaxTemporaryEgoPoints(EgoDomain) ceiling} — <em>not</em> a directly-held balance
     * accumulated from zero, which is what this returned before the two-pool model landed.
     */
    @Override
    public int getTemporaryEgoPoints(final EgoDomain domain) {
        return egoPoints.get(domain).getTemporaryRemaining(getPermanentEgoMax(domain), sumEgoPenalty(domain));
    }

    /** Everything domain can still pay with, from either pool. The affordability read. */
    @Override
    public int getAvailableEgoPoints(final EgoDomain domain) {
        return getPermanentEgoPoints(domain) + getTemporaryEgoPoints(domain);
    }

    /**
     * Spends up to amount points of type from domain, reporting what <em>actually</em> left the
     * pool.
     *
     * <p>The caller chooses the pool, and there is deliberately <strong>no fallback</strong> from
     * {@link EgoPointType#TEMPORARY} to {@link EgoPointType#PERMANENT} when temporary is short:
     * falling through would silently spend a permanent point, which costs twice over (see {@link
     * EgoPointPool}), and that ambiguity is exactly what an explicit type removes.
     *
     * <p>There is also deliberately <strong>no shorter overload</strong> defaulting type to
     * {@code TEMPORARY}. This codebase's cascading-overload convention is for an input that is
     * genuinely <em>optional</em>, delegating down with {@code null}; {@code type} is not
     * optional, it is the whole question this method exists to answer. Don't add one.
     *
     * <p>Floors at 0 rather than throwing — affordability is the caller's own check via {@link
     * #getAvailableEgoPoints}, matching this codebase's "possession is validated; eligibility
     * mostly isn't" restraint, and matching what {@code org.aventyrs.core.effect.Primor} needs,
     * since a critical hit isn't a transaction its victim can decline.
     */
    @Override
    public EgoPointSpend spendEgoPoints(final EgoDomain domain, final EgoPointType type, final int amount) {
        EgoPointPool pool = egoPoints.get(domain);
        int permanentMax = getPermanentEgoMax(domain);
        int spent = type == EgoPointType.PERMANENT
                ? pool.spendPermanent(permanentMax, amount)
                : pool.spendTemporary(permanentMax, sumEgoPenalty(domain), amount);
        applyEgoDepletionGrants(domain);
        return new EgoPointSpend(domain, type, spent);
    }

    /**
     * Schedules whatever any held ability owes this sheet for domain having just been reduced to
     * zero — {@code GnoseAbility#ESTABILIDADE_EMOCIONAL}'s temporary point. A no-op unless the
     * domain is genuinely empty (both pools), unless an ability actually reacts to it, and
     * unless this is the first time this session ({@link #consumeOncePerSession}).
     *
     * <p>Called from {@link #spendEgoPoints} because that is the one funnel <em>both</em> a
     * holder's deliberate use and {@code org.aventyrs.core.effect.Primor}'s drain pass through,
     * and "for reduzido a zero" doesn't care which emptied the pool — deliberately unlike
     * {@code EgoPointsService#useEgoPointsForEffect}, which exists precisely to keep a drain
     * from triggering a Vantagem.
     *
     * <p>Scans {@code attributeAbilities} only, not the usual three sources: this is a trigger,
     * not an aggregated stat, and the one clause that reacts to Ego depletion is an
     * {@code AttributeAbility}. Widen it when a second, differently-typed one exists.
     *
     * <p>One path to zero is deliberately not covered: a {@link TemporaryEgoPenalty} landing
     * (or a permanent Ego maximum dropping) can empty a domain without any spend, and {@link
     * #applyEffect} has no equivalent hook. No effect in this core empties a pool that way
     * today, and adding a second trigger site before one does would be guessing at its shape.
     */
    private void applyEgoDepletionGrants(final EgoDomain domain) {
        if (getAvailableEgoPoints(domain) > 0) {
            return;
        }
        for (AttributeAbility ability : character.getAttributeAbilities()) {
            int owed = ability.resolveEgoDepletionGrant(domain);
            if (owed > 0 && consumeOncePerSession(ability)) {
                scheduleTemporaryEgoPointGrant(domain, ability, owed);
            }
        }
    }

    /**
     * Restores up to amount previously-spent temporary points in domain, returning how many
     * actually came back — bounded by what was spent, so this can never push a pool past its own
     * ceiling. Named "recover", not "gain": since the two-pool model landed it cannot accumulate
     * past that ceiling, and "gain" would be a lie a caller only discovered at runtime.
     */
    @Override
    public int recoverTemporaryEgoPoints(final EgoDomain domain, final int amount) {
        return egoPoints.get(domain).recoverTemporary(getPermanentEgoMax(domain), sumEgoPenalty(domain), amount);
    }

    /**
     * Raises domain's temporary <em>ceiling</em> by amount, attributed to source and
     * non-cumulative per source — repeat grants from that <i>same</i> source don't stack past
     * what it already granted, while a different source's grant still adds on top. See {@link
     * EgoPointPool#grantTemporaryBonus}.
     */
    @Override
    public int grantTemporaryEgoPointBonus(final EgoDomain domain, final Object source, final int amount) {
        egoPoints.get(domain).grantTemporaryBonus(source, amount);
        return getTemporaryEgoPoints(domain);
    }

    /**
     * Widens domain's ceiling for source, then recovers under the widened ceiling — see {@link
     * CombatantSheet#grantTemporaryEgoPoints} for why an outright grant of temporary points
     * needs both halves, and why neither on its own survives an emptied pool.
     */
    @Override
    public int grantTemporaryEgoPoints(final EgoDomain domain, final Object source, final int amount) {
        grantTemporaryEgoPointBonus(domain, source, amount);
        recoverTemporaryEgoPoints(domain, amount);
        return getTemporaryEgoPoints(domain);
    }

    /**
     * Registers a {@link DelayedEgoGrant}; {@link #startNewRound()} delivers it. Doesn't grant
     * anything itself, the same "register now, resolve at the boundary" shape {@link
     * #owePendingEgoRecovery} has for a Rest.
     */
    @Override
    public void scheduleTemporaryEgoPointGrant(final EgoDomain domain, final Object source, final int amount) {
        scheduledEgoGrants.add(new DelayedEgoGrant(domain, source, amount));
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
     * Resolves every {@link PendingEgoRecovery} satisfied by a Rest of restType's tier, restoring
     * each one's owed temporary points, and discards them. The restoration is bounded by the
     * domain's own ceiling, like any other {@link #recoverTemporaryEgoPoints} call.
     */
    @Override
    public void applyPendingEgoRecoveries(final RestType restType) {
        pendingEgoRecoveries.removeIf(recovery -> {
            if (!recovery.isSatisfiedBy(restType)) {
                return false;
            }
            recoverTemporaryEgoPoints(recovery.getDomain(), recovery.getValue());
            return true;
        });
    }

    /**
     * Claims marker for this session, {@code true} only the first time — see {@link
     * CombatantSheet#consumeOncePerSession} for what a session is here (this sheet object's own
     * lifetime, hence the {@code transient} backing field).
     */
    @Override
    public boolean consumeOncePerSession(final Object marker) {
        return consumedSessionMarkers.add(marker);
    }

    @Override
    public boolean hasConsumedOncePerSession(final Object marker) {
        return consumedSessionMarkers.contains(marker);
    }

    /** Forgets every claimed marker, letting each once-per-session clause fire again. */
    @Override
    public void startNewSession() {
        consumedSessionMarkers.clear();
    }

    /**
     * Adds item to this sheet's {@link #getInventory()} — a plain mutator: it validates nothing
     * (no capacity check — no Carga concept exists). Paying for a purchase is {@code
     * org.aventyrs.core.character.services.ItemPurchaseService}'s job, via {@link
     * #spendEquipmentPoints(int)} then this method; a caller acquiring an item any other way
     * (loot, a GM grant, a craft) just calls this.
     */
    @Override
    public void addToInventory(final Item item) {
        inventory.add(item);
    }

    // --- Equipment Points (PE) ---------------------------------------------------------------

    /**
     * Adds amount Pontos de Equipamento to this sheet's balance, returning the new balance —
     * the mirror of {@code CharacterSheet#accumulateExperience}. A Narrador grant, or a starting
     * budget at creation. Rejects a negative amount.
     */
    public int grantEquipmentPoints(final int amount) {
        if (amount < 0) {
            throw new IllegalOperationException(NOT_ENOUGH_EQUIPMENT_POINTS);
        }
        return equipmentPoints += amount;
    }

    /**
     * Spends amount Pontos de Equipamento, returning the balance left. The subtraction happens
     * only once the result is known non-negative — the same order {@code
     * CharacterSheet#useExperience} keeps so a rejected spend never silently corrupts the balance.
     *
     * @throws IllegalOperationException ({@code NOT_ENOUGH_EQUIPMENT_POINTS}) if the balance is short
     */
    public int spendEquipmentPoints(final int amount) {
        int remaining = equipmentPoints - amount;
        if (remaining < 0) {
            throw new IllegalOperationException(NOT_ENOUGH_EQUIPMENT_POINTS);
        }
        return equipmentPoints = remaining;
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
        List<Condition> decaying = temporaryEffects.stream()
                .filter(effect -> effect instanceof Condition)
                .map(effect -> (Condition) effect)
                .filter(TemporaryEffect::isExpired)
                .filter(condition -> condition.getType().getDecaysTo() != null)
                .toList();
        // A lapsing Forma must put its holder back into their own shape before it is swept out —
        // the same "an expiring effect that must *do* something" case a decaying Condition gets.
        boolean formLapsed = temporaryEffects.stream()
                .anyMatch(effect -> effect instanceof FormEffect && effect.isExpired());
        temporaryEffects.removeIf(TemporaryEffect::isExpired);
        if (formLapsed) {
            enterForm(null);
        }
        // "Ao fim da duração alvo se torna Assustado" — the fear ladder steps down rather than
        // simply ending, so a decaying Condition is replaced by its successor at the moment it
        // expires, carrying the same origin and that successor's own stated duration. Applied
        // after the removal sweep so the successor isn't swept out in the same pass.
        decaying.forEach(condition -> applyCondition(new Condition(
                condition.getType().getDecaysTo(),
                ConditionType.DEFAULT_FEAR_DURATION_IN_ROUNDS,
                condition.getSource())));
    }

    /**
     * Whatever this combatant's Raça shrugs off — {@code Race#getCriticalEffectImmunities()},
     * empty for all but {@code Troll}'s Anatomia Vegetal today. A combatant is otherwise
     * vulnerable to every Efeito Crítico, and only an authored anatomy clause changes that.
     *
     * <p>{@code MonsterSheet} overrides this rather than adding to it: a foe's anatomy is
     * authored wholesale on its {@code MonsterTemplate}, and its {@code Character}'s race is the
     * single catch-all {@code Monstruoso} (which grants none), so there is nothing of the race's
     * to lose. A player-<i>acquired</i> trait still has no path here — see {@code
     * ProfissaoCompetencyAbility}'s still-unbuilt Resistência a Críticos, which would need a
     * scan over held abilities rather than this one flat lookup.
     */
    @Override
    public Set<CriticalEffectType> getCriticalEffectImmunities() {
        return getCharacter().getRace().getCriticalEffectImmunities();
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
     * the same convention {@code ActionPointsService}/{@code ActionProfile} use.
     *
     * <p>Resets {@link #movementsTakenThisRound} (see {@link #consumeMovementThisRound()}) and
     * marks where this Turn's actions begin in {@link #actionsThisRound} — the per-Rodada log
     * itself is <em>not</em> cleared here (a Reação taken on another combatant's Turn belongs to
     * this Rodada), only at {@link #startNewRound()}. {@link #isFirstRollOfTurnFor} reads the
     * slice after this marker, which is what keeps {@code DexterityAbility#PRECISAO}'s "primeira
     * rolagem... em cada um de seus Turnos" per-<em>Turn</em>. Nothing else triggers "no início
     * do seu turno" yet ({@link Bleeding}/{@link ManaDrain}/{@link Withering} all apply at
     * Turn-<em>end</em>), so turnNumber stays unused for now, still in hand for whatever plugs in
     * next.
     */
    @Override
    public void startTurn(final int turnNumber) {
        movementsTakenThisRound = 0;
        actionCountAtTurnStart = actionsThisRound.size();
        drewWeaponThisTurn = false;
    }

    /**
     * Begins a new Rodada: clears the action log and resets the per-Turn marker. Called by
     * {@code Scene#next()} on every active participant at the Rodada wrap; without a live {@code
     * Scene} the log simply starts empty, so a "first this Rodada" clause still reads correctly —
     * the same fallback {@link #consumeMovementThisRound()} has, and the API is expected to call
     * this at its own Rodada boundary.
     */
    @Override
    public void startNewRound() {
        actionsThisRound.clear();
        actionCountAtTurnStart = 0;
        applyScheduledEgoGrants();
        tickCooldowns();
    }

    /**
     * Burns one Rodada off every Resfriamento still owed, dropping the ones that reach zero so
     * the ledger stays exactly "what is still unavailable". Private, and driven only from the
     * Rodada boundary — a Resfriamento measured in Rodadas must not tick at Turn end, or it
     * would come back early for whoever acts late in the order.
     */
    private void tickCooldowns() {
        cooldowns.replaceAll((ability, remaining) -> remaining - 1);
        cooldowns.values().removeIf(remaining -> remaining <= 0);
    }

    /**
     * Puts ability on Resfriamento for rounds Rodadas — called by {@code
     * ActiveAbilityService#activate} once the activation has actually succeeded, never before,
     * so a refused activation costs nothing. A rounds of 0 or less clears any existing entry
     * rather than storing one, since "no Resfriamento" and "Resfriamento already elapsed" are
     * the same state.
     */
    @Override
    public void startCooldown(final ActiveAbility ability, final int rounds) {
        if (rounds <= 0) {
            cooldowns.remove(ability);
            return;
        }
        cooldowns.put(ability, rounds);
    }

    @Override
    public int getRemainingCooldown(final ActiveAbility ability) {
        return cooldowns.getOrDefault(ability, 0);
    }

    @Override
    public void startRestCooldown(final ActiveAbility ability, final RestType restType) {
        if (restType == null) {
            restCooldowns.remove(ability);
            return;
        }
        restCooldowns.put(ability, restType);
    }

    @Override
    public boolean isAwaitingRest(final ActiveAbility ability) {
        return restCooldowns.containsKey(ability);
    }

    /** A Descanso frees everything waiting on its own tier or a weaker one. */
    @Override
    public void clearRestCooldowns(final RestType restType) {
        restCooldowns.values().removeIf(required -> restType.isAtLeast(required));
    }

    /**
     * Delivers every {@link DelayedEgoGrant} scheduled during the Rodada just ended, and clears
     * them — the "na Rodada seguinte" half of {@code GnoseAbility#ESTABILIDADE_EMOCIONAL}.
     * Private: a grant is registered through {@link #scheduleTemporaryEgoPointGrant} and lands
     * at the one Rodada boundary, never on demand.
     */
    private void applyScheduledEgoGrants() {
        List<DelayedEgoGrant> due = List.copyOf(scheduledEgoGrants);
        scheduledEgoGrants.clear();
        due.forEach(grant -> grantTemporaryEgoPoints(grant.getDomain(), grant.getSource(), grant.getValue()));
    }

    /**
     * Begins a new Cena: clears both action logs and the per-Cena drawn-weapon flag. {@code
     * Scene#addParticipant} calls this when the sheet joins; the API calls it otherwise.
     */
    @Override
    public void startNewScene() {
        actionsThisCena.clear();
        actionsThisRound.clear();
        actionCountAtTurnStart = 0;
        drewWeaponThisScene = false;
        combatStarted = false;
    }

    /**
     * Resolves and applies this combatant's start-of-combat Talento Blessings — see {@link
     * CombatantSheet#startCombat()}. Scans {@link #getCharacter()}'s Talentos (the sheet acting
     * on its own Character, the same way it reads {@code getFeats()} for every other resolve
     * pass), grants each {@link Blessing} as a {@link TemporaryBonus}, and returns them.
     * A no-op returning an empty list if already fired this Cena.
     */
    @Override
    public List<Blessing> startCombat() {
        if (combatStarted) {
            return List.of();
        }
        combatStarted = true;
        List<Blessing> granted = new ArrayList<>();
        for (Feat feat : getCharacter().getFeats()) {
            for (Blessing blessing : feat.resolveCombatStartBlessings(getCharacter())) {
                grantTemporaryBonus(blessing.getModifierType(), blessing.getValue(), blessing.getRounds());
                granted.add(blessing);
            }
        }
        return granted;
    }

    /**
     * Appends action to this Rodada's log (and this Cena's). Called explicitly by the API after
     * it has resolved a roll (or an {@code AttackDelivery}/{@code AttackReceiver} exchange) —
     * never from inside {@code AbstractSkillInteraction#applyTo}, which only <em>reads</em> the
     * log. A caller with a live {@code Scene} reaches this through {@code Scene#recordAction},
     * which also files the action in the Scene's permanent history.
     */
    @Override
    public void recordAction(final CombatantAction action) {
        actionsThisRound.add(action);
        actionsThisCena.add(action);
    }

    @Override
    public List<CombatantAction> getActionsThisRound() {
        return Collections.unmodifiableList(actionsThisRound);
    }

    @Override
    public List<CombatantAction> getActionsThisCena() {
        return Collections.unmodifiableList(actionsThisCena);
    }

    @Override
    public boolean hasDrawnWeaponThisScene() {
        return drewWeaponThisScene;
    }

    @Override
    public boolean isFirstRollOfTurnFor(final AttributeDomain domain) {
        return actionsThisRound.subList(actionCountAtTurnStart, actionsThisRound.size()).stream()
                .noneMatch(action -> action.governingDomain() == domain);
    }

    @Override
    public boolean isFirstAttackRollOfTurn() {
        return actionsThisRound.subList(actionCountAtTurnStart, actionsThisRound.size()).stream()
                .noneMatch(action -> action.skill() != null && action.skill().isAttackSkill());
    }

    @Override
    public int getMovementsTakenThisRound() {
        return movementsTakenThisRound;
    }

    /**
     * Reset by {@link #startTurn}, which is what makes this per-<i>Rodada</i> despite living on a
     * Turn boundary: each participant has one Turn per Rodada, the equivalence {@link
     * #finishTurn()} already relies on. Without a live {@code Scene} ever calling {@code
     * startTurn}, the counter simply starts at 0, so the first movement still correctly reads as
     * first.
     */
    @Override
    public int consumeMovementThisRound() {
        return movementsTakenThisRound++;
    }
    // --- Condições / Malefícios ---------------------------------------------------------------

    /**
     * Replaces any held {@link Condition} naming the same {@link ConditionType} before adding
     * this one, which is narrower than {@link #applyEffect}'s own {@code isCumulative()} handling
     * — that compares by {@code getClass()}, and every Condition shares one class, so going
     * through it would let a new Desprevenido silently lift an unrelated Silêncio.
     */
    @Override
    public void applyCondition(final Condition condition) {
        removeCondition(condition.getType());
        temporaryEffects.add(condition);
    }

    @Override
    public void removeCondition(final ConditionType conditionType) {
        temporaryEffects.removeIf(effect -> effect instanceof Condition held
                && held.getType() == conditionType);
    }

    /** Held, unexpired Conditions — the directly-applied ones, before implications. */
    private Stream<Condition> heldConditions() {
        return temporaryEffects.stream()
                .filter(effect -> effect instanceof Condition)
                .map(effect -> (Condition) effect)
                .filter(condition -> !condition.isExpired());
    }

    /**
     * Walks each held Condition's {@link ConditionType#getImplied()} graph, keeping an implied
     * entry only while its own {@link Range} scope holds against that Condition's origin. The
     * walk is iterative with a seen-set so a cycle in the catalogue cannot hang it — nothing
     * authored today implies its way back around, but a data table is the wrong place to rely on
     * that.
     */
    @Override
    public Set<ConditionType> getActiveConditions(final SceneContext sceneContext) {
        return activeConditionOrigins(sceneContext).keySet();
    }

    /**
     * Every {@link ConditionType} in force mapped to the held {@link Condition} that put it
     * there — its own instance for a directly-applied one, or whichever implied it. <b>Keyed by
     * type, so a condition conferred by two different sources appears once</b>: being Desprevenido
     * because you are both Caído and Flanqueado is not worse than being Desprevenido, and summing
     * per held Condition instead would charge its -2 Defesas twice. Where two sources imply the
     * same condition, the first encountered supplies the origin any range-scoped effect of that
     * condition measures against; nothing authored today implies a range-scoped effect from two
     * places, so no precedence is invented.
     */
    private Map<ConditionType, Condition> activeConditionOrigins(final SceneContext sceneContext) {
        Map<ConditionType, Condition> active = new EnumMap<>(ConditionType.class);
        heldConditions().forEach(held -> collectConditions(held, held.getType(), sceneContext, active));
        return active;
    }

    private void collectConditions(final Condition held, final ConditionType type,
                                    final SceneContext sceneContext, final Map<ConditionType, Condition> active) {
        if (active.putIfAbsent(type, held) != null) {
            return;
        }
        type.getImplied().forEach((implied, within) -> {
            if (held.appliesWithin(within, sceneContext)) {
                collectConditions(held, implied, sceneContext, active);
            }
        });
    }

    @Override
    public boolean hasCondition(final ConditionType conditionType, final SceneContext sceneContext) {
        return activeConditionOrigins(sceneContext).containsKey(conditionType);
    }

    /**
     * Sums each active condition's effects <b>once</b>, resolving any proximity scope against the
     * origin of whichever held Condition put it in force — so an implied condition brings its
     * numbers with it (Caído really does cost 2 Defesas through the Desprevenido it confers)
     * without a second source of the same condition charging them again.
     */
    @Override
    public int getConditionBonus(final ModifierType modifierType, final SceneContext sceneContext) {
        return activeConditionOrigins(sceneContext).entrySet().stream()
                .mapToInt(entry -> new Condition(entry.getKey(), null, entry.getValue().getSource())
                        .resolveBonus(modifierType, sceneContext))
                .sum();
    }

    /**
     * Applies {@link ConditionType#DESARMADO} only once <b>no</b> wielded {@link Weapon} remains.
     * A fighter holding two blades who loses one is not Desarmado — the condition's Desvantagem
     * on every Ataque and Dano roll is the penalty for having nothing to fight with, and charging
     * it to someone still holding a sword would plainly overshoot. The rules text states the
     * condition's effects, not when it is inflicted, so this reading is ours; it is the narrow
     * one.
     *
     * <p>Open-ended (a {@code null} duration): being disarmed ends by picking a weapon back up,
     * which is {@link #rearm(Weapon)}, never by counting down Rodadas.
     */
    @Override
    public boolean drawWeapon(final Weapon weapon) {
        boolean drawn = getCharacter().drawWeapon(weapon);
        if (drawn) {
            drewWeaponThisTurn = true;
            drewWeaponThisScene = true;
        }
        return drawn;
    }

    @Override
    public boolean hasDrawnWeaponThisTurn() {
        return drewWeaponThisTurn;
    }

    @Override
    public java.util.Optional<Weapon> disarm(final Weapon weapon) {
        if (!weapon.isDisarmable() || !getCharacter().unequip(weapon)) {
            return java.util.Optional.empty();
        }
        if (wieldsNoWeapon()) {
            applyCondition(new Condition(ConditionType.DESARMADO, null));
        }
        return java.util.Optional.of(weapon);
    }

    @Override
    public boolean rearm(final Weapon weapon) {
        if (anyConditionPrevents(null, ConditionType::preventsArming)) {
            return false;
        }
        getCharacter().equip(weapon);
        removeCondition(ConditionType.DESARMADO);
        return true;
    }

    private boolean wieldsNoWeapon() {
        return getCharacter().getEquipment().stream().noneMatch(item -> item instanceof Weapon);
    }

    @Override
    public boolean canAttackWith(final Weapon weapon) {
        if (weapon == null) {
            return true;
        }
        return !anyConditionPrevents(null, ConditionType::restrictsAttacksToLightWeapons)
                || weapon.getEffectiveWeightClass() == ItemWeightClass.LIGHT;
    }

    @Override
    public int getAttackerDamageBonusFromConditions(final SceneContext sceneContext) {
        return activeConditionOrigins(sceneContext).keySet().stream()
                .mapToInt(ConditionType::getAttackerDamageBonus)
                .sum();
    }

    /**
     * Every source of Resistência a Críticos this combatant has, summed additively: the standing
     * grants from its Raça and its held Talentos, plus whatever round-scoped {@link
     * ModifierType#CRITICAL_RESISTANCE} bonus is in force. Two sources, not the usual three-source
     * {@code @Modifier} scan — no {@code AttributeAbility} or {@code SkillCompetencyAbility} in
     * the catalog grants a standing RC ({@code ProfissaoCompetencyAbility#FORJA_VULCANA}'s is
     * scoped to a <em>produced item</em>, not to its holder, so it needs a per-copy value rather
     * than a hook here), and per this codebase's "second real consumer" restraint neither hook is
     * added ahead of one.
     *
     * <p>Additive because RC instances stack: each is a separate -2, the same way two sources of
     * RD sum. The subtraction itself, and the floor on it, live on the attacker's crit path — see
     * {@link CombatantSheet#getTotalCriticalResistance}.
     */
    @Override
    public int getTotalCriticalResistance(final SceneContext sceneContext) {
        int total = getCharacter().getRace().getCriticalResistance();
        total += getCharacter().getFeats().stream()
                .mapToInt(feat -> feat.resolveCriticalResistance(getCharacter(), sceneContext, this))
                .sum();
        return total + getTemporaryBonus(ModifierType.CRITICAL_RESISTANCE);
    }

    @Override
    public FormType getCurrentForm() {
        return currentForm;
    }

    /**
     * Takes form, or returns to this combatant's own shape when it is {@code null}. Validates
     * nothing — see {@link CombatantSheet#enterForm} for why, and {@link #canTakeForm} for the
     * question a caller asks first.
     */
    @Override
    public FormType enterForm(final FormType form) {
        FormType previous = currentForm;
        currentForm = form;
        return previous;
    }

    /**
     * Combines every held Talento's {@code Feat#resolveFormAccess}: one {@link
     * FormAccess#FORBIDDEN} refuses, and a {@link FormAccess#REQUIRED} refuses anything that is
     * not the shape it demands. A holder of two Talentos requiring <em>different</em> shapes can
     * take neither, which is the honest reading — the rules avoid the situation by making such
     * Talentos mutually exclusive ({@code GorgonaFeat}'s pairs), and {@code
     * FeatRequirements#forbiddenFeats} now enforces that, so this is a state the catalog cannot
     * actually produce.
     */
    @Override
    public boolean canTakeForm(final FormType form) {
        return !isForbidden(form) && isNotLockedElsewhere(form);
    }

    /** Whether some held Talento refuses form outright. */
    private boolean isForbidden(final FormType form) {
        return getCharacter().getFeats().stream()
                .anyMatch(feat -> feat.resolveFormAccess(form, getCharacter()) == FormAccess.FORBIDDEN);
    }

    /**
     * Whether no held Talento locks its holder into some shape <em>other</em> than form. A
     * locking Talento refuses everything but the one shape it names — including the holder's own,
     * which is why {@code form} being {@code null} is refused too rather than treated as neutral.
     * Vacuously true when nothing locks, which is every character but a Górgona with Marca da
     * Maldição.
     */
    private boolean isNotLockedElsewhere(final FormType form) {
        for (Feat feat : getCharacter().getFeats()) {
            for (FormType locked : FormType.values()) {
                if (locked != form
                        && feat.resolveFormAccess(locked, getCharacter()) == FormAccess.REQUIRED) {
                    return false;
                }
            }
        }
        return true;
    }

    /** True while any active condition forbids the thing predicate names. */
    private boolean anyConditionPrevents(final SceneContext sceneContext,
                                          final java.util.function.Predicate<ConditionType> predicate) {
        return activeConditionOrigins(sceneContext).keySet().stream().anyMatch(predicate);
    }

    @Override
    public boolean isMovementPrevented(final SceneContext sceneContext) {
        return anyConditionPrevents(sceneContext, ConditionType::preventsMovement);
    }

    @Override
    public boolean isHealingPrevented() {
        return anyConditionPrevents(null, ConditionType::preventsHealing);
    }

    @Override
    public boolean isAbilityActivationPrevented(final SceneContext sceneContext) {
        return anyConditionPrevents(sceneContext, ConditionType::preventsAbilityActivation);
    }

    @Override
    public boolean isSpellCastingPrevented(final SceneContext sceneContext) {
        return anyConditionPrevents(sceneContext, ConditionType::preventsSpellCasting);
    }
}
