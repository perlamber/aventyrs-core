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
}
