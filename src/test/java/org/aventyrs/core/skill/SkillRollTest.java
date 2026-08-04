package org.aventyrs.core.skill;

import org.aventyrs.core.sheet.IllegalOperationException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SkillRollTest {

    @Test
    void getTotalSumsAllThreeDice() {
        SkillRoll roll = new SkillRoll(List.of(2, 3, 5));

        assertEquals(10, roll.getTotal());
    }

    @Test
    void rejectsFewerThanThreeDice() {
        assertThrows(IllegalOperationException.class, () -> new SkillRoll(List.of(3, 4)));
    }

    @Test
    void rejectsMoreThanThreeDice() {
        assertThrows(IllegalOperationException.class, () -> new SkillRoll(List.of(1, 2, 3, 4)));
    }

    @Test
    void rejectsAFaceBelowOne() {
        assertThrows(IllegalOperationException.class, () -> new SkillRoll(List.of(0, 2, 3)));
    }

    @Test
    void rejectsAFaceAboveSix() {
        assertThrows(IllegalOperationException.class, () -> new SkillRoll(List.of(2, 3, 7)));
    }

    @Test
    void tripleOnesIsFalhaCriticaMaior() {
        SkillRoll roll = new SkillRoll(List.of(1, 1, 1));

        assertEquals(CriticalResult.FALHA_CRITICA_MAIOR, roll.getCriticalResult());
    }

    @Test
    void twoOnesIsFalhaCriticaMenor() {
        SkillRoll roll = new SkillRoll(List.of(1, 1, 4));

        assertEquals(CriticalResult.FALHA_CRITICA_MENOR, roll.getCriticalResult());
    }

    @Test
    void tripleSixesIsAcertoCriticoMaior() {
        SkillRoll roll = new SkillRoll(List.of(6, 6, 6));

        assertEquals(CriticalResult.ACERTO_CRITICO_MAIOR, roll.getCriticalResult());
    }

    @Test
    void twoSixesIsAcertoCriticoMenor() {
        SkillRoll roll = new SkillRoll(List.of(6, 2, 6));

        assertEquals(CriticalResult.ACERTO_CRITICO_MENOR, roll.getCriticalResult());
    }

    @Test
    void noMatchingExtremeIsNone() {
        SkillRoll roll = new SkillRoll(List.of(2, 4, 5));

        assertEquals(CriticalResult.NONE, roll.getCriticalResult());
    }

    @Test
    void aSingleOneOrSixIsNotCritical() {
        assertEquals(CriticalResult.NONE, new SkillRoll(List.of(1, 3, 4)).getCriticalResult());
        assertEquals(CriticalResult.NONE, new SkillRoll(List.of(6, 3, 4)).getCriticalResult());
    }
}
