package org.aventyrs.core.character;

import org.aventyrs.core.sheet.IllegalOperationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DamageBaseTest {

    /**
     * The whole scale, row by row: value climbs to 3, rolls over into a die and resets, until
     * both caps are reached at 3d6+3 — after which every further scale-up is a flat +2.
     */
    @ParameterizedTest
    @CsvSource({
            " 0, 1, 0",
            " 1, 1, 1",
            " 2, 1, 2",
            " 3, 1, 3",
            " 4, 2, 0",
            " 5, 2, 1",
            " 6, 2, 2",
            " 7, 2, 3",
            " 8, 3, 0",
            " 9, 3, 1",
            "10, 3, 2",
            "11, 3, 3",
            "12, 3, 5",
            "13, 3, 7",
            "14, 3, 9",
            "15, 3, 11",
            "16, 3, 13",
    })
    void theScaleMatchesTheTable(int scale, int expectedDice, int expectedValue) {
        DamageBase damageBase = new DamageBase(scale);
        assertEquals(expectedDice, damageBase.diceCount());
        assertEquals(expectedValue, damageBase.value());
    }

    @Test
    void unarmedIsTheBottomOfTheScale() {
        assertEquals(0, DamageBase.UNARMED.scale());
        assertEquals(1, DamageBase.UNARMED.diceCount());
        assertEquals(0, DamageBase.UNARMED.value());
    }

    @Test
    void aNegativeScaleClampsToUnarmed() {
        assertEquals(DamageBase.UNARMED, new DamageBase(-7));
    }

    @Test
    void scalingUpAdvancesRowByRow() {
        assertEquals(new DamageBase(4), DamageBase.UNARMED.scaledUp(4));
        assertEquals(new DamageBase(11), new DamageBase(4).scaledUp(7));
    }

    @Test
    void scalingUpNeverFallsBelowUnarmed() {
        assertEquals(DamageBase.UNARMED, new DamageBase(2).scaledUp(-9));
    }

    /** The overflow branch is open-ended — nothing caps how far past 3d6+3 the scale runs. */
    @Test
    void scalingUpPastBothCapsAddsTwoEachTime() {
        DamageBase capped = DamageBase.of(3, 3);
        assertEquals(5, capped.scaledUp(1).value());
        assertEquals(105, capped.scaledUp(51).value());
        assertEquals(3, capped.scaledUp(51).diceCount());
    }

    @Test
    void isFullyCappedOnlyFromThreeD6PlusThreeOn() {
        assertFalse(new DamageBase(10).isFullyCapped());
        assertTrue(new DamageBase(11).isFullyCapped());
        assertTrue(new DamageBase(12).isFullyCapped());
    }

    @Test
    void ofResolvesAnAuthoredNotationToItsScale() {
        assertEquals(0, DamageBase.of(1, 0).scale());
        assertEquals(4, DamageBase.of(2, 0).scale());
        assertEquals(7, DamageBase.of(2, 3).scale());
        assertEquals(11, DamageBase.of(3, 3).scale());
    }

    @Test
    void ofRefusesANotationOutsideTheAuthorableRegion() {
        assertThrows(IllegalOperationException.class, () -> DamageBase.of(0, 0));
        assertThrows(IllegalOperationException.class, () -> DamageBase.of(4, 0));
        assertThrows(IllegalOperationException.class, () -> DamageBase.of(1, -1));
        assertThrows(IllegalOperationException.class, () -> DamageBase.of(3, 5));
    }

    @Test
    void toStringUsesTheUsualNotation() {
        assertEquals("1d6+0", DamageBase.UNARMED.toString());
        assertEquals("2d6+1", new DamageBase(5).toString());
        assertEquals("3d6+7", new DamageBase(13).toString());
    }

    /**
     * Two Dano Bases are equal exactly when they sit on the same row — the scale is the only
     * stored component, so an unreachable pairing like 2d6+7 can't be constructed at all.
     */
    @Test
    void equalityIsPositionOnTheScale() {
        assertEquals(new DamageBase(6), DamageBase.of(2, 2));
        assertEquals(new DamageBase(6).hashCode(), DamageBase.of(2, 2).hashCode());
    }
}
