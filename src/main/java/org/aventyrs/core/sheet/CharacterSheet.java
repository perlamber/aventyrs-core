package org.aventyrs.core.sheet;

import org.aventyrs.core.character.Character;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import static org.aventyrs.core.util.TranslatableMessages.NOT_ENOUGH_EXPERIENCE;

@RequiredArgsConstructor(staticName="of")
@Getter
public class CharacterSheet {
    @NonNull
    private Character character;
    @NonNull
    private Player player;

    private BigDecimal totalExperience = BigDecimal.ZERO;

    private BigDecimal unUsedExperience = BigDecimal.ZERO;

    private int damageTaken = 0;

    private int shieldPoints = 0;

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
        return damageTaken += remaining;
    }

    /**
     * Applies curse damage, which drains life directly and bypasses Shield points.
     * @return int total damage accumulated so far
     */
    public int applyCurseDamage(int amount)
    {
        return damageTaken += amount;
    }

    /**
     * Heals accumulated damage.
     * @return int remaining damage accumulated
     */
    public int heal(int amount)
    {
        return damageTaken = Math.max(0, damageTaken - amount);
    }

    /**
     * Grants Shield points, absorbed before damage reaches the character's Hit Points.
     * @return int total Shield points available
     */
    public int addShield(int amount)
    {
        return shieldPoints += amount;
    }
}
