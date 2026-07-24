package org.aventyrs.core.character.services;

import org.aventyrs.core.ability.StrengthAbility;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AttributeAbilityServiceTest {

    private final AttributeAbilityService abilityService = new AttributeAbilityServiceImpl();

    @Test
    void noSlotsUnlockedBelowBaseThree() {
        assertEquals(0, abilityService.getUnlockedAbilitySlots(2));
    }

    @Test
    void oneSlotUnlockedFromBaseThree() {
        assertEquals(1, abilityService.getUnlockedAbilitySlots(3));
        assertEquals(1, abilityService.getUnlockedAbilitySlots(4));
    }

    @Test
    void twoSlotsUnlockedFromBaseFive() {
        assertEquals(2, abilityService.getUnlockedAbilitySlots(5));
        assertEquals(2, abilityService.getUnlockedAbilitySlots(6));
    }

    @Test
    void allowsFirstChoiceAtBaseThree() {
        assertDoesNotThrow(() -> abilityService.validateChoice(3, List.of(), StrengthAbility.SUBJUGAR));
    }

    @Test
    void rejectsChoiceWithNoUnlockedSlot() {
        assertThrows(IllegalOperationException.class,
                () -> abilityService.validateChoice(2, List.of(), StrengthAbility.SUBJUGAR));
    }

    @Test
    void rejectsChoosingTheSameAbilityTwice() {
        assertThrows(IllegalOperationException.class,
                () -> abilityService.validateChoice(5, List.of(StrengthAbility.SUBJUGAR), StrengthAbility.SUBJUGAR));
    }

    @Test
    void rejectsSecondChoiceUntilBaseFive() {
        assertThrows(IllegalOperationException.class,
                () -> abilityService.validateChoice(4, List.of(StrengthAbility.SUBJUGAR), StrengthAbility.MOVIMENTO_LIVRE));
    }

    @Test
    void allowsSecondDistinctChoiceAtBaseFive() {
        assertDoesNotThrow(() -> abilityService.validateChoice(5, List.of(StrengthAbility.SUBJUGAR), StrengthAbility.MOVIMENTO_LIVRE));
    }
}
