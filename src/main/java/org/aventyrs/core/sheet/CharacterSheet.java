package org.aventyrs.core.sheet;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.util.NoValueException;

import java.util.Optional;
import java.util.function.IntPredicate;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(staticName="of")
@Getter
public class CharacterSheet {
    @NonNull
    private Character character;
    @NonNull
    private Player player;
    private Integer availableExperience = 0;

    public Integer getTotalExperience()
    {
        return availableExperience + character.getUsedExperience();
    }

    public Integer useExperience(Integer expToUse) throws IllegalOperationException
    {
        if(Optional.ofNullable(expToUse).orElseThrow(NoValueException::new).compareTo(availableExperience) > 0)
        {
            throw new IllegalOperationException("Illegal value introduced for experience");
        }
        else
        {
            character.accumulateExperience(expToUse);
            availableExperience -= expToUse;
        }
        return availableExperience;
    }
}
