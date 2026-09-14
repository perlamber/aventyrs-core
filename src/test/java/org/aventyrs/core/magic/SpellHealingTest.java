package org.aventyrs.core.magic;

import org.aventyrs.core.rest.RestType;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpellHealingTest {

    @Test
    void restEquivalentCarriesItsTierAndNothingElse() {
        SpellHealing healing = SpellHealing.restEquivalent(RestType.LONGO);

        assertEquals(RestType.LONGO, healing.restEquivalent());
        assertFalse(healing.fullRecovery());
        assertFalse(healing.halvedForHostiles());
        assertEquals(HealingCondition.ALWAYS, healing.condition());
    }

    @Test
    void fullRecoveryNamesNoRestTier() {
        assertNull(SpellHealing.FULL_RECOVERY.restEquivalent());
        assertTrue(SpellHealing.FULL_RECOVERY.fullRecovery());
    }

    @Test
    void statingBothAMagnitudeAndAFullRecoveryIsAnAuthoringMistake() {
        assertThrows(IllegalOperationException.class,
                () -> new SpellHealing(RestType.LONGO, true, false, HealingCondition.ALWAYS));
    }

    @Test
    void statingNeitherMagnitudeIsAnAuthoringMistake() {
        assertThrows(IllegalOperationException.class,
                () -> new SpellHealing(null, false, false, HealingCondition.ALWAYS));
    }

    @Test
    void aMissingConditionIsAnAuthoringMistakeRatherThanADefault() {
        assertThrows(IllegalOperationException.class,
                () -> new SpellHealing(RestType.LONGO, false, false, null));
    }

    @Test
    void eachWitherKeepsTheMagnitudeAndChangesOnlyItsOwnColumn() {
        SpellHealing halved = SpellHealing.restEquivalent(RestType.LONGO).halvingForHostiles();
        SpellHealing gated = SpellHealing.restEquivalent(RestType.MINIMO).onlyIfNotBleeding();

        assertEquals(RestType.LONGO, halved.restEquivalent());
        assertTrue(halved.halvedForHostiles());
        assertEquals(HealingCondition.ALWAYS, halved.condition());

        assertEquals(RestType.MINIMO, gated.restEquivalent());
        assertEquals(HealingCondition.ONLY_IF_NOT_BLEEDING, gated.condition());
        assertFalse(gated.halvedForHostiles());
    }
}
