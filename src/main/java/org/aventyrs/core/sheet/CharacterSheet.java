package org.aventyrs.core.sheet;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterStatus;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.rest.RestType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import static org.aventyrs.core.util.TranslatableMessages.NOT_ENOUGH_EXPERIENCE;

@RequiredArgsConstructor(staticName="of")
@Getter
public class CharacterSheet implements Interactable<CharacterSheet> {
    /**
     * A unique, stable identifier for this specific CharacterSheet instance — distinct from
     * {@link Character#getId()}, since the same Character could in principle back more than
     * one CharacterSheet. {@link org.aventyrs.core.scene.Scene} keys its participants by this,
     * not by object-reference equality. Auto-generated for a newly created CharacterSheet via
     * {@link #of(Character, Player)}; pass an already-known one via
     * {@link #of(Character, Player, UUID)} instead when reconstructing a CharacterSheet from
     * persisted state (e.g. a DTO loaded from storage), where the identity already exists and
     * must be preserved rather than re-minted. Not {@code final} for exactly that reason, but
     * there's still no public setter — the only way to set it is at construction, through one
     * of those two factories.
     */
    private UUID id = UUID.randomUUID();

    @NonNull
    private Character character;
    @NonNull
    private Player player;

    private BigDecimal totalExperience = BigDecimal.ZERO;

    private BigDecimal unUsedExperience = BigDecimal.ZERO;

    @Getter(AccessLevel.NONE)
    private final ResourcePool hitPoints = new ResourcePool();

    @Getter(AccessLevel.NONE)
    private final ResourcePool magicPoints = new ResourcePool();

    @Getter(AccessLevel.NONE)
    private final ResourcePool determinationPoints = new ResourcePool();

    @Getter(AccessLevel.NONE)
    private final Map<EgoDomain, TemporaryPointPool> temporaryEgoPoints = newTemporaryEgoPointsPools();

    private int shieldPoints = 0;

    private int famaPositiva = 0;

    private int famaNegativa = 0;

    /**
     * Every {@link TemporaryEffect} (a {@link TemporaryBonus} or a {@link Bleeding}) this
     * CharacterSheet is currently holding — one shared list, since both count down in
     * Rodadas the same way and must be ticked together (see {@link
     * #tickTemporaryEffects()}) rather than risking a double-decrement from two separate
     * tick methods walking the same kind of state independently.
     */
    @Getter(AccessLevel.NONE)
    private final List<TemporaryEffect> temporaryEffects = new ArrayList<>();

    /**
     * Temporary Ego points owed back to this CharacterSheet at its next qualifying Rest
     * — see {@link PendingEgoRecovery}, {@link #owePendingEgoRecovery}, and {@link
     * #applyPendingEgoRecoveries}.
     */
    @Getter(AccessLevel.NONE)
    private final List<PendingEgoRecovery> pendingEgoRecoveries = new ArrayList<>();

    private static Map<EgoDomain, TemporaryPointPool> newTemporaryEgoPointsPools() {
        Map<EgoDomain, TemporaryPointPool> pools = new EnumMap<>(EgoDomain.class);
        for (EgoDomain domain : EgoDomain.values()) {
            pools.put(domain, new TemporaryPointPool());
        }
        return pools;
    }

    /**
     * Same as {@link #of(Character, Player)}, but with a known id instead of a freshly
     * minted one — for reconstructing a CharacterSheet from persisted state (e.g. a DTO)
     * whose identity already exists.
     */
    public static CharacterSheet of(final Character character, final Player player, @NonNull final UUID id) {
        CharacterSheet sheet = of(character, player);
        sheet.id = id;
        return sheet;
    }

    /**
     * Consumes the available experience
     * @param expToUse experience to be used
     * @return BigDecimal remaining experience
     * @throws IllegalOperationException in case unUsed experience is lower than consumed
     */
    public BigDecimal useExperience(BigDecimal expToUse) throws IllegalOperationException
    {
        BigDecimal remainingExperience = unUsedExperience.subtract(expToUse);
        if(remainingExperience.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalOperationException(NOT_ENOUGH_EXPERIENCE);
        return unUsedExperience = remainingExperience;
    }

    public BigDecimal accumulateExperience(BigDecimal experience)
    {
        unUsedExperience = unUsedExperience.add(experience);
        return totalExperience = totalExperience.add(experience);
    }

    public int getDamageTaken()
    {
        return hitPoints.getSpent();
    }

    public int getManaSpent()
    {
        return magicPoints.getSpent();
    }

    public int getDeterminationSpent()
    {
        return determinationPoints.getSpent();
    }

    /**
     * Applies damage, consuming any Shield points first.
     * @return int total damage accumulated so far
     */
    public int applyDamage(int amount)
    {
        int remaining = amount;
        if (shieldPoints > 0)
        {
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
    public int applyCurseDamage(int amount)
    {
        return hitPoints.spend(amount);
    }

    /**
     * Heals accumulated damage — the same recovery a Rest applies to PV. Also interrupts
     * every active {@link Bleeding} (Sangramento's own rules text: "Efeitos de cura
     * interrompem a perda de PV por rodada") — the immediate PV Sangramento already dealt
     * stays lost; only the ongoing per-Rodada loss stops.
     * @return int remaining damage accumulated
     */
    public int heal(int amount)
    {
        if (amount > 0)
        {
            temporaryEffects.removeIf(effect -> effect instanceof Bleeding);
        }
        return hitPoints.recover(amount);
    }

    /**
     * Grants Shield points, absorbed before damage reaches the character's Hit Points.
     * @return int total Shield points available
     */
    public int addShield(int amount)
    {
        return shieldPoints += amount;
    }

    /**
     * Spends Magic Points, e.g. to cast a spell.
     * @return int total Magic Points spent so far
     */
    public int spendMagicPoints(int amount)
    {
        return magicPoints.spend(amount);
    }

    /**
     * Recovers spent Magic Points — the same recovery a Rest applies to PM. Also
     * interrupts every active {@link ManaDrain} (ManaPurge's own rules text: "Efeitos de
     * cura interrompem a perda de PM por rodada") — the immediate PM ManaPurge already
     * drained stays lost; only the ongoing per-Rodada loss stops.
     * @return int remaining Magic Points spent
     */
    public int recoverMagicPoints(int amount)
    {
        if (amount > 0)
        {
            temporaryEffects.removeIf(effect -> effect instanceof ManaDrain);
        }
        return magicPoints.recover(amount);
    }

    /**
     * Spends Determination Points, e.g. to activate an ability.
     * @return int total Determination Points spent so far
     */
    public int spendDeterminationPoints(int amount)
    {
        return determinationPoints.spend(amount);
    }

    /**
     * Recovers spent Determination Points — the same recovery a Rest applies to PD.
     * @return int remaining Determination Points spent
     */
    public int recoverDeterminationPoints(int amount)
    {
        return determinationPoints.recover(amount);
    }

    /**
     * Current temporary points held in the given Ego — gained piecemeal (e.g. Narrador
     * rewards) and spent for small, temporary advantages. See {@link TemporaryPointPool}.
     */
    public int getTemporaryEgoPoints(EgoDomain domain)
    {
        return temporaryEgoPoints.get(domain).getAmount();
    }

    /**
     * Gains temporary points in the given Ego.
     * @return int total temporary points held in that Ego after the gain
     */
    public int gainTemporaryEgoPoints(EgoDomain domain, int amount)
    {
        return temporaryEgoPoints.get(domain).gain(amount);
    }

    /**
     * Spends temporary points from the given Ego for a small, temporary advantage. Never
     * goes below zero.
     * @return int remaining temporary points held in that Ego after the spend
     */
    public int spendTemporaryEgoPoints(EgoDomain domain, int amount)
    {
        return temporaryEgoPoints.get(domain).spend(amount);
    }

    /**
     * Registers a {@link PendingEgoRecovery} — e.g. {@code org.aventyrs.core.effect.Primor}'s
     * own promise that the temporary Ego points it just spent come back at the target's
     * next qualifying Rest. Doesn't itself spend or gain anything; the caller spends the
     * points via {@link #spendTemporaryEgoPoints} before calling this to register their
     * eventual return.
     */
    public void owePendingEgoRecovery(final PendingEgoRecovery recovery)
    {
        pendingEgoRecoveries.add(recovery);
    }

    /**
     * Resolves every {@link PendingEgoRecovery} satisfied by a Rest of restType's tier —
     * granting each one's owed points back via {@link #gainTemporaryEgoPoints} — and
     * discards them. Called by {@code RestServiceImpl#applyRest} whenever a Rest is
     * actually taken; unlike {@link #tickTemporaryEffects()}'s still-nonexistent Round
     * trigger, {@code RestService} already is the real, complete "a Rest happened" hook.
     */
    public void applyPendingEgoRecoveries(final RestType restType)
    {
        pendingEgoRecoveries.stream()
                .filter(recovery -> recovery.isSatisfiedBy(restType))
                .forEach(recovery -> gainTemporaryEgoPoints(recovery.getDomain(), recovery.getValue()));
        pendingEgoRecoveries.removeIf(recovery -> recovery.isSatisfiedBy(restType));
    }

    @Override
    public InteractionResult receiveInteraction(Interaction<CharacterSheet> interaction)
    {
        return interaction.applyTo(this);
    }

    /**
     * Increases Fama Positiva — e.g. an Excelência bonus or a Narrador reward. Randomly
     * triggered increases and direct calls both go through this method.
     * @return int total Fama Positiva after the increase
     */
    public int increaseFamaPositiva(int amount)
    {
        return famaPositiva += amount;
    }

    /**
     * Increases Fama Negativa — e.g. an Excelência bonus or a Narrador reward. Randomly
     * triggered increases and direct calls both go through this method.
     * @return int total Fama Negativa after the increase
     */
    public int increaseFamaNegativa(int amount)
    {
        return famaNegativa += amount;
    }

    /**
     * Registers a {@link TemporaryEffect} — a {@link TemporaryBonus} (e.g. {@code
     * ArtesCompetencyAbility#DOM_BARDICO} motivating an ally) or a {@link Bleeding} (e.g.
     * {@code org.aventyrs.core.effect.Sangramento}), or any future kind alike. Doesn't
     * apply any immediate effect itself — e.g. Sangramento's own immediate PV loss is
     * applied directly via {@link #applyDamage(int)} before this is called for its
     * ongoing half.
     *
     * <p>When {@code effect.isCumulative()} is false (e.g. {@link Withering}, from
     * {@code org.aventyrs.core.effect.Definhar}), any existing instance of the same
     * concrete type is removed first — reapplying replaces it (a fresh duration/value)
     * rather than stacking a second one alongside it.
     */
    public void applyEffect(final TemporaryEffect effect)
    {
        if (!effect.isCumulative())
        {
            temporaryEffects.removeIf(existing -> existing.getClass() == effect.getClass());
        }
        temporaryEffects.add(effect);
    }

    /**
     * Grants a {@link TemporaryBonus} of type — e.g. {@code ArtesCompetencyAbility
     * #DOM_BARDICO} motivating an ally, granting them (not the caster) a
     * {@link ModifierType#SKILL_ROLL_BONUS} for a few Rodadas. The granting Character isn't
     * tracked here (nothing about this mechanism needs to know who granted it); locate
     * targets via {@link org.aventyrs.core.scene.Scene#getAllies}.
     * @return int the total of type's active temporary bonuses after granting this one
     */
    public int grantTemporaryBonus(final ModifierType type, final int value, final int rounds)
    {
        applyEffect(new TemporaryBonus(type, value, rounds));
        return getTemporaryBonus(type);
    }

    /**
     * The sum of every currently-active (non-expired) {@link TemporaryBonus} of type this
     * CharacterSheet is holding — e.g. what a {@code <Skill>Interaction} should add into its
     * own {@link ModifierType#SKILL_ROLL_BONUS} total alongside the Character's permanent
     * abilities/excellencies.
     */
    public int getTemporaryBonus(final ModifierType type)
    {
        return temporaryEffects.stream()
                .filter(effect -> effect instanceof TemporaryBonus)
                .map(effect -> (TemporaryBonus) effect)
                .filter(bonus -> !bonus.isExpired())
                .filter(bonus -> bonus.getType() == type)
                .mapToInt(TemporaryBonus::getValue)
                .sum();
    }

    /**
     * Advances every held {@link TemporaryEffect} by one Rodada: applies each one's own
     * per-Rodada side effect (e.g. {@link Bleeding}/{@link ManaDrain} draining PV/PM, via
     * {@link TemporaryEffect#applyRoundEffect}), then counts every effect down and
     * discards any that expire as a result. This is a single method over the shared
     * {@link #temporaryEffects} list rather than one per effect kind, since ticking each
     * kind separately would decrement the other kinds' Rodadas too every time any one was
     * called — and dispatches through the polymorphic {@code applyRoundEffect} hook
     * rather than {@code instanceof}-filtering per kind, so a future {@code
     * TemporaryEffect} subclass needs no change here to be ticked correctly.
     */
    public void tickTemporaryEffects()
    {
        temporaryEffects.forEach(effect -> effect.applyRoundEffect(this));
        temporaryEffects.forEach(TemporaryEffect::tick);
        temporaryEffects.removeIf(TemporaryEffect::isExpired);
    }

    /**
     * Ends this CharacterSheet's own Turn — everything that needs to happen once this
     * Turn is over. Currently just advances its {@link TemporaryEffect}s by one Rodada
     * (via {@link #tickTemporaryEffects()}); each participant only has one Turn per
     * Rodada, so ticking once per that participant's own Turn ending is exactly "once per
     * Rodada" from this CharacterSheet's perspective. Expected to grow further as more
     * per-Turn bookkeeping is added (e.g. resetting per-Turn resources) — {@code void}
     * rather than reporting any one of those pieces (like Bleeding's own PV loss) back to
     * the caller, since it's meant to be a single catch-all end-of-Turn hook, not a
     * one-off computation. Not called automatically by anything yet — {@link
     * org.aventyrs.core.scene.Scene} doesn't have a "turn shifter" that calls this on the
     * participant whose Turn just ended as it advances to the next one (see {@link
     * org.aventyrs.core.scene.Scene#next()}); once it does, this is the method it's
     * expected to call.
     */
    public void finishTurn()
    {
        tickTemporaryEffects();
    }
}
