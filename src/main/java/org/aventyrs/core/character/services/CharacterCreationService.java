package org.aventyrs.core.character.services;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterEgos;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.feat.Feat;
import org.aventyrs.core.feat.StartingFeatSlot;
import org.aventyrs.core.race.Race;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;

import java.util.List;
import java.util.Map;

public interface CharacterCreationService {
    int STARTING_ATTRIBUTE_POINTS = 7;
    int MAX_STARTING_ATTRIBUTE_BASE = 3;

    /** Every Ego starts with 2 points before the player's single extra point is placed. */
    int STARTING_EGO_POINTS = 2;

    /** The single extra point a player must place among the 4 Egos at creation. */
    int EXTRA_EGO_POINTS = 1;

    /**
     * Minimum Ego base, reached through creation-time allocation, to unlock that Ego's own
     * Vantagem de Ego — the same threshold for every {@link EgoDomain}, not a separate value
     * per domain.
     */
    int EGO_ADVANTAGE_MIN_BASE = 3;

    /** How many General Talentos every character starts with, before anything its Raça grants. */
    int DEFAULT_GENERAL_FEAT_SLOTS = 2;

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
     * Whether domain's Vantagem de Ego may be chosen: only when that domain's base resolved
     * by {@link #allocateEgos} itself reached {@value #EGO_ADVANTAGE_MIN_BASE}. A base raised
     * to that value afterwards (Talentos, Títulos Aventyrs, other Habilidades) never grants
     * this — those sources add to {@link org.aventyrs.core.character.EgoValue#getVariable()},
     * not to {@link org.aventyrs.core.character.EgoValue#getBase()}. One generic method for
     * every {@link EgoDomain} rather than a separate {@code isXAdvantageAvailable} method per
     * domain (an earlier version had exactly that — {@code isAutocontroleAdvantageAvailable}/
     * {@code isInitiativeAdvantageAvailable} — before the threshold was confirmed identical
     * across domains), mirroring {@link CharacterEgos#getEgo} and
     * {@link org.aventyrs.core.character.Character#getEgoAdvantage} already being generic
     * over {@code EgoDomain} rather than one field/method per domain.
     */
    boolean isEgoAdvantageAvailable(EgoDomain domain, CharacterEgos egos);

    /**
     * Every Talento character picks at creation, in order: {@value #DEFAULT_GENERAL_FEAT_SLOTS}
     * General slots, then {@link Race#getStartingFeatSlots()}. The order is the one {@link
     * #grantStartingFeats} matches picks against.
     */
    List<StartingFeatSlot> getStartingFeatSlots(Race race);

    /**
     * Every authored Talento character may put in slot right now — admitted by the slot, eligible
     * for character as the slot judges it, and not already held. Sheet-less, so the Fama and
     * EXP-total prerequisites are skipped rather than failed; see the overload.
     *
     * <p>Picks can unlock one another (a {@code requiredFeat}), so a client that grants picks
     * one by one should ask again after each.
     */
    default List<Feat> getStartingFeatOptions(final Character character, final StartingFeatSlot slot) {
        return getStartingFeatOptions(character, slot, null);
    }

    /** The same list, with sheet's Fama and EXP-total prerequisites enforced when sheet is given. */
    List<Feat> getStartingFeatOptions(Character character, StartingFeatSlot slot, CharacterSheet sheet);

    /**
     * Grants picks free of XP, {@code picks.get(i)} filling {@code
     * getStartingFeatSlots(character.getRace()).get(i)}. Each pick is checked against the
     * character as the earlier picks left it, so a later pick may require an earlier one. A
     * choice-carrying Talento is passed in its acquired form, as with {@link
     * FeatService#grantFeat}.
     *
     * <p>Not atomic: picks before a refused one stay granted, the same as calling {@code
     * FeatService#grantFeat} in a loop. Validate with {@link #getStartingFeatOptions} first to
     * avoid a partial grant.
     *
     * @throws IllegalOperationException {@code INVALID_STARTING_FEAT_SELECTION} if picks doesn't
     *                                   have one entry per slot or a pick isn't among its slot's
     *                                   options; {@code FEAT_REQUIRES_CHOICE} if a pick is a bare
     *                                   constant that needs a choice
     */
    default void grantStartingFeats(final Character character, final List<Feat> picks) throws IllegalOperationException {
        grantStartingFeats(character, picks, null);
    }

    /** The same grant, with sheet's Fama and EXP-total prerequisites enforced when sheet is given. No XP is spent. */
    void grantStartingFeats(Character character, List<Feat> picks, CharacterSheet sheet) throws IllegalOperationException;
}
