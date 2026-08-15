package org.aventyrs.core.skill;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CriticalResultTest {

    @Test
    void isCriticalSuccessIsTrueForMenorAndMaior() {
        assertTrue(CriticalResult.ACERTO_CRITICO_MENOR.isCriticalSuccess());
        assertTrue(CriticalResult.ACERTO_CRITICO_MAIOR.isCriticalSuccess());
    }

    @Test
    void isCriticalSuccessIsFalseForNoneAndBothFailures() {
        assertFalse(CriticalResult.NONE.isCriticalSuccess());
        assertFalse(CriticalResult.FALHA_CRITICA_MENOR.isCriticalSuccess());
        assertFalse(CriticalResult.FALHA_CRITICA_MAIOR.isCriticalSuccess());
    }
}
