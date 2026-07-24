package org.aventyrs.core.character.services;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.Race;
import org.aventyrs.core.sheet.IllegalOperationException;

import java.util.Map;

public interface CharacterCreationService {
    int STARTING_ATTRIBUTE_POINTS = 7;
    int MAX_STARTING_ATTRIBUTE_BASE = 3;

    /**
     * Resolves a character's starting {@link CharacterAttributes} from the player's choices.
     *
     * @param race                        the character's race, defining fixed and choosable racial bonuses
     * @param basePointAllocation         points assigned to each attribute's base, on top of the natural
     *                                    minimum of 1; must add up to {@value #STARTING_ATTRIBUTE_POINTS} and
     *                                    never push a base above {@value #MAX_STARTING_ATTRIBUTE_BASE}
     * @param chosenRacialBonusAllocation how the race's choosable racial bonus points are assigned; must add
     *                                    up to {@link Race#getChoosableAttributeBonusPoints()} and only target
     *                                    attributes in {@link Race#getChoosableAttributes()}
     * @throws IllegalOperationException if either allocation is invalid
     */
    CharacterAttributes allocateAttributes(Race race,
                                            Map<AttributeDomain, Integer> basePointAllocation,
                                            Map<AttributeDomain, Integer> chosenRacialBonusAllocation) throws IllegalOperationException;
}
