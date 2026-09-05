package org.aventyrs.core.item;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ItemWeightClassTest {

    @Test
    void aprimoramentoSlotsAreOnePerWeightTier() {
        assertEquals(1, ItemWeightClass.LIGHT.getMaximumImprovements());
        assertEquals(2, ItemWeightClass.MEDIUM.getMaximumImprovements());
        assertEquals(3, ItemWeightClass.HEAVY.getMaximumImprovements());
    }

    @Test
    void adjustedByStaysWithinTheAuthoredRange() {
        assertEquals(ItemWeightClass.LIGHT, ItemWeightClass.LIGHT.adjustedBy(-2));
        assertEquals(ItemWeightClass.HEAVY, ItemWeightClass.HEAVY.adjustedBy(3));
        assertEquals(ItemWeightClass.MEDIUM, ItemWeightClass.LIGHT.adjustedBy(1));
    }
}
