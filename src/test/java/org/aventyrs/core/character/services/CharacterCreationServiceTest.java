package org.aventyrs.core.character.services;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.Human;
import org.aventyrs.core.character.Race;
import org.aventyrs.core.sheet.DlcRuleset;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.character.Character;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CharacterCreationServiceTest {

    private final CharacterCreationService creationService = new CharacterCreationServiceImpl();

    /** A race with no fixed bonus and no choosable bonus, same baseline as Human. */
    private final Race noBonusRace = new Human();

    /** A race with a fixed bonus, e.g. mountain-dwellers hardened by their environment. */
    private final Race fixedBonusRace = new Race() {
        @Override
        public Map<AttributeDomain, Integer> getFixedAttributeBonuses() {
            return Map.of(AttributeDomain.VIGOR, 1);
        }

        @Override
        public Character.CharacterBuilder generateEmptyCharacter(List<DlcRuleset> dlcRulesetList) {
            return Character.builder();
        }
    };

    /** A race that lets the player choose 1 point of bonus between two attributes. */
    private final Race choosableBonusRace = new Race() {
        @Override
        public int getChoosableAttributeBonusPoints() {
            return 1;
        }

        @Override
        public Set<AttributeDomain> getChoosableAttributes() {
            return Set.of(AttributeDomain.DEXTERITY, AttributeDomain.INSTINCT);
        }

        @Override
        public Character.CharacterBuilder generateEmptyCharacter(List<DlcRuleset> dlcRulesetList) {
            return Character.builder();
        }
    };

    @Test
    void distributesSevenPointsAcrossAllAttributeBases() throws IllegalOperationException {
        Map<AttributeDomain, Integer> allocation = Map.of(
                AttributeDomain.VIGOR, 1, AttributeDomain.STRENGTH, 1, AttributeDomain.DEXTERITY, 1,
                AttributeDomain.FOCUS, 1, AttributeDomain.INSTINCT, 1, AttributeDomain.GNOSE, 1,
                AttributeDomain.CHARISMA, 1);

        CharacterAttributes attributes = creationService.allocateAttributes(noBonusRace, allocation, Map.of());

        assertEquals(2, attributes.getVigor().getBase());
        assertEquals(2, attributes.getCharisma().getBase());
        assertEquals(0, attributes.getVigor().getRacialBonus());
    }

    @Test
    void rejectsAllocationThatDoesNotSumToSevenPoints() {
        Map<AttributeDomain, Integer> allocation = Map.of(AttributeDomain.VIGOR, 1, AttributeDomain.STRENGTH, 1);

        assertThrows(IllegalOperationException.class,
                () -> creationService.allocateAttributes(noBonusRace, allocation, Map.of()));
    }

    @Test
    void rejectsAnyAttributeBaseAboveThreeAtCreation() {
        Map<AttributeDomain, Integer> allocation = Map.of(
                AttributeDomain.VIGOR, 3, AttributeDomain.STRENGTH, 1, AttributeDomain.DEXTERITY, 1,
                AttributeDomain.FOCUS, 1, AttributeDomain.INSTINCT, 1);

        assertThrows(IllegalOperationException.class,
                () -> creationService.allocateAttributes(noBonusRace, allocation, Map.of()));
    }

    @Test
    void appliesFixedRacialBonusAutomatically() throws IllegalOperationException {
        Map<AttributeDomain, Integer> allocation = Map.of(
                AttributeDomain.VIGOR, 1, AttributeDomain.STRENGTH, 1, AttributeDomain.DEXTERITY, 1,
                AttributeDomain.FOCUS, 1, AttributeDomain.INSTINCT, 1, AttributeDomain.GNOSE, 1,
                AttributeDomain.CHARISMA, 1);

        CharacterAttributes attributes = creationService.allocateAttributes(fixedBonusRace, allocation, Map.of());

        assertEquals(1, attributes.getVigor().getRacialBonus());
        assertEquals(0, attributes.getStrength().getRacialBonus());
    }

    @Test
    void appliesChosenRacialBonusToAnEligibleAttribute() throws IllegalOperationException {
        Map<AttributeDomain, Integer> allocation = Map.of(
                AttributeDomain.VIGOR, 1, AttributeDomain.STRENGTH, 1, AttributeDomain.DEXTERITY, 1,
                AttributeDomain.FOCUS, 1, AttributeDomain.INSTINCT, 1, AttributeDomain.GNOSE, 1,
                AttributeDomain.CHARISMA, 1);

        CharacterAttributes attributes = creationService.allocateAttributes(
                choosableBonusRace, allocation, Map.of(AttributeDomain.DEXTERITY, 1));

        assertEquals(1, attributes.getDexterity().getRacialBonus());
        assertEquals(0, attributes.getInstinct().getRacialBonus());
    }

    @Test
    void rejectsChosenRacialBonusOnAnIneligibleAttribute() {
        Map<AttributeDomain, Integer> allocation = Map.of(
                AttributeDomain.VIGOR, 1, AttributeDomain.STRENGTH, 1, AttributeDomain.DEXTERITY, 1,
                AttributeDomain.FOCUS, 1, AttributeDomain.INSTINCT, 1, AttributeDomain.GNOSE, 1,
                AttributeDomain.CHARISMA, 1);

        assertThrows(IllegalOperationException.class, () -> creationService.allocateAttributes(
                choosableBonusRace, allocation, Map.of(AttributeDomain.CHARISMA, 1)));
    }

    @Test
    void rejectsRacialBonusAllocationNotMatchingAvailablePoints() {
        Map<AttributeDomain, Integer> allocation = Map.of(
                AttributeDomain.VIGOR, 1, AttributeDomain.STRENGTH, 1, AttributeDomain.DEXTERITY, 1,
                AttributeDomain.FOCUS, 1, AttributeDomain.INSTINCT, 1, AttributeDomain.GNOSE, 1,
                AttributeDomain.CHARISMA, 1);

        assertThrows(IllegalOperationException.class, () -> creationService.allocateAttributes(
                choosableBonusRace, allocation, Map.of()));
    }
}
