package org.aventyrs.core.character.services;

import org.aventyrs.core.ability.AcquiredChoice;
import org.aventyrs.core.character.Character;

import java.util.Optional;

public class AbilityChoiceServiceImpl implements AbilityChoiceService {

    @Override
    @SuppressWarnings("unchecked")
    public <C> Optional<C> getChoiceFor(final Character character, final Object ability) {
        return character.getAbilityChoices().stream()
                .filter(choice -> choice.getAbility().equals(ability))
                .findFirst()
                .map(choice -> (C) choice.getValue());
    }
}
