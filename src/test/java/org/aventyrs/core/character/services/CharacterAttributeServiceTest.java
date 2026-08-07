package org.aventyrs.core.character.services;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CharacterAttributeServiceTest {

    private final CharacterAttributeService attributeService = new CharacterAttributeServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private CharacterSheet sheetWithExperience(final BigDecimal experience) {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        sheet.accumulateExperience(experience);
        return sheet;
    }

    @Test
    void upgradeCostIsTargetBasePlusOne() {
        AttributeValue value = AttributeValue.builder().domain(AttributeDomain.VIGOR).base(2).build();
        assertEquals(BigDecimal.valueOf(4), attributeService.getUpgradeCost(value));
    }

    @Test
    void upgradeBaseIncreasesBaseByOnePreservingOtherComponents() throws IllegalOperationException {
        AttributeValue value = AttributeValue.builder().domain(AttributeDomain.VIGOR).base(2).racialBonus(1).variable(1).build();
        CharacterSheet sheet = sheetWithExperience(BigDecimal.valueOf(10));

        AttributeValue upgraded = attributeService.upgradeBase(value, sheet);

        assertEquals(3, upgraded.getBase());
        assertEquals(1, upgraded.getRacialBonus());
        assertEquals(1, upgraded.getVariable());
    }

    @Test
    void upgradeBaseSpendsTheUpgradeCostFromTheCharacterSheet() throws IllegalOperationException {
        AttributeValue value = AttributeValue.builder().domain(AttributeDomain.VIGOR).base(2).build();
        CharacterSheet sheet = sheetWithExperience(BigDecimal.valueOf(10));

        attributeService.upgradeBase(value, sheet);

        // Upgrade cost for base 2->3 is 4 (target 3 + 1); 10 - 4 = 6.
        assertEquals(BigDecimal.valueOf(6), sheet.getUnUsedExperience());
    }

    @Test
    void upgradeBaseRejectsGoingAboveMaximumOfFive() {
        AttributeValue value = AttributeValue.builder().domain(AttributeDomain.VIGOR).base(5).build();
        CharacterSheet sheet = sheetWithExperience(BigDecimal.valueOf(100));

        assertThrows(IllegalOperationException.class, () -> attributeService.upgradeBase(value, sheet));
    }

    @Test
    void upgradeBaseRejectsWhenNotEnoughExperienceAndDoesNotSpendAnyway() {
        AttributeValue value = AttributeValue.builder().domain(AttributeDomain.VIGOR).base(2).build();
        CharacterSheet sheet = sheetWithExperience(BigDecimal.valueOf(3));

        assertThrows(IllegalOperationException.class, () -> attributeService.upgradeBase(value, sheet));
        assertEquals(BigDecimal.valueOf(3), sheet.getUnUsedExperience());
    }
}
