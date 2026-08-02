package org.aventyrs.core.sheet;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterStatus;
import org.aventyrs.core.character.EgoDomain;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import static org.aventyrs.core.util.TranslatableMessages.NOT_ENOUGH_EXPERIENCE;

@RequiredArgsConstructor(staticName="of")
@Getter
public class CharacterSheet implements Interactable<CharacterSheet> {
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

    private static Map<EgoDomain, TemporaryPointPool> newTemporaryEgoPointsPools() {
        Map<EgoDomain, TemporaryPointPool> pools = new EnumMap<>(EgoDomain.class);
        for (EgoDomain domain : EgoDomain.values()) {
            pools.put(domain, new TemporaryPointPool());
        }
        return pools;
    }

    /**
     * Consumes the available experience
     * @param expToUse experience to be used
     * @return BigDecimal remaining experience
     * @throws IllegalOperationException in case unUsed experience is lower than consumed
     */
    public BigDecimal useExperience(BigDecimal expToUse) throws IllegalOperationException
    {
        BigDecimal remainingExperience = unUsedExperience = unUsedExperience.subtract(expToUse);
        if(remainingExperience.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalOperationException(NOT_ENOUGH_EXPERIENCE);
        return remainingExperience;
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
     * Heals accumulated damage — the same recovery a Rest applies to PV.
     * @return int remaining damage accumulated
     */
    public int heal(int amount)
    {
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
     * Recovers spent Magic Points — the same recovery a Rest applies to PM.
     * @return int remaining Magic Points spent
     */
    public int recoverMagicPoints(int amount)
    {
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
}
