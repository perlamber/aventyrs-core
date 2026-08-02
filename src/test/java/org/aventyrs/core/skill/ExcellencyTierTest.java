package org.aventyrs.core.skill;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExcellencyTierTest {

    @Test
    void thresholdsMatchTheThreeUniversalTiers() {
        assertEquals(3, ExcellencyTier.FOCADO.getRequiredGraduation());
        assertEquals(7, ExcellencyTier.PRODIGIO.getRequiredGraduation());
        assertEquals(10, ExcellencyTier.LENDA.getRequiredGraduation());
    }

    @Test
    void isUnlockedByRespectsTheThreshold() {
        assertFalse(ExcellencyTier.FOCADO.isUnlockedBy(2));
        assertTrue(ExcellencyTier.FOCADO.isUnlockedBy(3));
        assertTrue(ExcellencyTier.FOCADO.isUnlockedBy(10));
    }

    @Test
    void unlockedByReturnsEveryTierReachedInAscendingOrder() {
        assertEquals(List.of(), ExcellencyTier.unlockedBy(2));
        assertEquals(List.of(ExcellencyTier.FOCADO), ExcellencyTier.unlockedBy(5));
        assertEquals(List.of(ExcellencyTier.FOCADO, ExcellencyTier.PRODIGIO), ExcellencyTier.unlockedBy(7));
        assertEquals(List.of(ExcellencyTier.FOCADO, ExcellencyTier.PRODIGIO, ExcellencyTier.LENDA),
                ExcellencyTier.unlockedBy(10));
    }
}
