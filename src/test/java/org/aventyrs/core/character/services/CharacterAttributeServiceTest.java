package org.aventyrs.core.character.services;

import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CharacterAttributeServiceTest {

    private final CharacterAttributeService attributeService = new CharacterAttributeServiceImpl();

    @Test
    void upgradeCostIsTargetBasePlusOne() {
        AttributeValue value = AttributeValue.builder().base(2).build();
        assertEquals(BigDecimal.valueOf(4), attributeService.getUpgradeCost(value));
    }

    @Test
    void upgradeBaseIncreasesBaseByOnePreservingOtherComponents() throws IllegalOperationException {
        AttributeValue value = AttributeValue.builder().base(2).racialBonus(1).variable(1).build();

        AttributeValue upgraded = attributeService.upgradeBase(value);

        assertEquals(3, upgraded.getBase());
        assertEquals(1, upgraded.getRacialBonus());
        assertEquals(1, upgraded.getVariable());
    }

    @Test
    void upgradeBaseRejectsGoingAboveMaximumOfFive() {
        AttributeValue value = AttributeValue.builder().base(5).build();

        assertThrows(IllegalOperationException.class, () -> attributeService.upgradeBase(value));
    }
}
