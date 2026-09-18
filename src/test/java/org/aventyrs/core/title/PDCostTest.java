package org.aventyrs.core.title;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PDCostTest {

    @Test
    void aFixedCostAcceptsExactlyItsValue() {
        PDCost cost = PDCost.fixed(2);

        assertTrue(cost.accepts(2));
        assertFalse(cost.accepts(1));
        assertFalse(cost.accepts(3));
        assertEquals(2, cost.minimum());
        assertFalse(cost.isVariable());
    }

    @Test
    void aVariableCostAcceptsAnythingFromItsMinimumUp() {
        PDCost cost = PDCost.variable(1);

        assertTrue(cost.accepts(1));
        assertTrue(cost.accepts(5));
        assertFalse(cost.accepts(0));
        assertEquals(1, cost.minimum());
        assertTrue(cost.isVariable());
    }

    @Test
    void noneIsAFixedCostOfZero() {
        assertEquals(PDCost.fixed(0), PDCost.NONE);
        assertTrue(PDCost.NONE.accepts(0));
        assertFalse(PDCost.NONE.accepts(1));
    }

    @Test
    void costsAreComparedByValue() {
        assertEquals(PDCost.variable(1), PDCost.variable(1));
        assertFalse(PDCost.fixed(1).equals(PDCost.variable(1)));
    }

    @Test
    void invalidAmountsAreRefused() {
        assertThrows(IllegalArgumentException.class, () -> PDCost.fixed(-1));
        assertThrows(IllegalArgumentException.class, () -> PDCost.variable(0));
    }

    @Test
    void theInterfaceDefaultIsNoCost() {
        AventyrTitleAbility ability = new AventyrTitleAbility() {
            @Override
            public String getDescription() {
                return "";
            }

            @Override
            public java.util.Optional<Class<? extends org.aventyrs.core.sheet.Interaction>> getInteractionClass() {
                return java.util.Optional.empty();
            }
        };

        assertEquals(PDCost.NONE, ability.getPDCost());
    }
}
