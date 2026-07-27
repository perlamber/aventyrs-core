package org.aventyrs.core.character.services;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterEgos;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.character.Race;
import org.aventyrs.core.sheet.IllegalOperationException;

import java.util.Map;

public interface CharacterCreationService {
    int STARTING_ATTRIBUTE_POINTS = 7;
    int MAX_STARTING_ATTRIBUTE_BASE = 3;

    /** Every Ego starts with 2 points before the player's single extra point is placed. */
    int STARTING_EGO_POINTS = 2;

    /** The single extra point a player must place among the 4 Egos at creation. */
    int EXTRA_EGO_POINTS = 1;

    /** Minimum Autocontrole base, reached through creation-time allocation, to unlock a Vantagem de Autocontrole. */
    int AUTOCONTROLE_ADVANTAGE_MIN_BASE = 3;

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

    /**
     * Resolves a character's starting {@link CharacterEgos}: {@value #STARTING_EGO_POINTS}
     * points in every Ego, plus the player's single extra point placed on one of them.
     *
     * <p>Not modeled: the alternative creation-time trade described in the ruleset
     * (reducing one Ego to 1 to raise another to 3) — its interaction with this mandatory
     * extra point isn't unambiguous from the rules text alone.
     *
     * @param extraPointAllocation where the single extra point goes; must add up to exactly
     *                             {@value #EXTRA_EGO_POINTS} across all four {@link EgoDomain}s
     * @throws IllegalOperationException if the allocation doesn't add up to exactly the extra point
     */
    CharacterEgos allocateEgos(Map<EgoDomain, Integer> extraPointAllocation) throws IllegalOperationException;

    /**
     * Whether a Vantagem de Autocontrole may be chosen: only when the Autocontrole base
     * resolved by {@link #allocateEgos} itself reached {@value #AUTOCONTROLE_ADVANTAGE_MIN_BASE}.
     * A base raised to that value afterwards (Talentos, Títulos Aventyrs, other Habilidades)
     * never grants this — those sources add to {@link org.aventyrs.core.character.EgoValue#getVariable()},
     * not to {@link org.aventyrs.core.character.EgoValue#getBase()}.
     */
    boolean isAutocontroleAdvantageAvailable(CharacterEgos egos);
}
