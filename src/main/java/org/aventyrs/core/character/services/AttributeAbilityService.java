package org.aventyrs.core.character.services;

import org.aventyrs.core.ability.AttributeAbility;
import org.aventyrs.core.sheet.IllegalOperationException;

import java.util.List;

public interface AttributeAbilityService {
    int FIRST_ABILITY_ATTRIBUTE_BASE = 3;
    int SECOND_ABILITY_ATTRIBUTE_BASE = 5;

    /**
     * How many attribute abilities a base of this magnitude has unlocked: 0 below
     * {@value #FIRST_ABILITY_ATTRIBUTE_BASE}, 1 from there on, 2 from
     * {@value #SECOND_ABILITY_ATTRIBUTE_BASE} on.
     */
    int getUnlockedAbilitySlots(int attributeBase);

    /**
     * Validates picking a new attribute ability: it must not already be chosen, and there
     * must be an unused slot unlocked by the attribute's current base.
     *
     * @throws IllegalOperationException if the ability was already chosen or no slot is free
     */
    void validateChoice(int attributeBase, List<AttributeAbility> alreadyChosen, AttributeAbility choice) throws IllegalOperationException;
}
