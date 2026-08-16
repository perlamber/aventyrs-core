package org.aventyrs.core.character.services;

import org.aventyrs.core.ability.AttributeAbility;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.sheet.IllegalOperationException;

import java.util.List;

import static org.aventyrs.core.util.TranslatableMessages.ATTRIBUTE_ABILITY_ALREADY_CHOSEN;
import static org.aventyrs.core.util.TranslatableMessages.NO_ATTRIBUTE_ABILITY_SLOT_AVAILABLE;

public class AttributeAbilityServiceImpl implements AttributeAbilityService {

    @Override
    public int getUnlockedAbilitySlots(final int attributeBase) {
        if (attributeBase >= SECOND_ABILITY_ATTRIBUTE_BASE) {
            return 2;
        }
        if (attributeBase >= FIRST_ABILITY_ATTRIBUTE_BASE) {
            return 1;
        }
        return 0;
    }

    @Override
    public void validateChoice(final int attributeBase, final List<AttributeAbility> alreadyChosen, final AttributeAbility choice) throws IllegalOperationException {
        if (alreadyChosen.contains(choice)) {
            throw new IllegalOperationException(ATTRIBUTE_ABILITY_ALREADY_CHOSEN);
        }
        long chosenOfSameDomain = alreadyChosen.stream()
                .filter(ability -> ability.getAttributeDomain() == choice.getAttributeDomain())
                .count();
        if (chosenOfSameDomain >= getUnlockedAbilitySlots(attributeBase)) {
            throw new IllegalOperationException(NO_ATTRIBUTE_ABILITY_SLOT_AVAILABLE);
        }
    }

    @Override
    public Character grantAttributeAbility(final Character character, final AttributeAbility ability) throws IllegalOperationException {
        int attributeBase = character.getAttributes().getAttribute(ability.getAttributeDomain()).getBase();
        validateChoice(attributeBase, character.getAttributeAbilities(), ability);

        Character.CharacterBuilder builder = character.toBuilder().attributeAbility(ability);
        ability.resolvePermanentEgoGain().ifPresent(domain ->
                builder.egos(character.getEgos().withVariableBonus(domain, 1)));
        ability.resolveActiveAbility().ifPresent(builder::activeAbility);
        return builder.build();
    }
}
