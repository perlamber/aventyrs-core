package org.aventyrs.core.skill;

import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.skill.artes.ArtesCompetencyAbility;
import org.aventyrs.core.skill.attention.AttentionSpecialization;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
    void oneOneOneTwoIsFalhaCriticaMenor() {
        SkillRoll roll = new SkillRoll(List.of(1, 1, 2));

        assertEquals(CriticalResult.FALHA_CRITICA_MENOR, roll.getCriticalResult());
    }

    /**
     * Regression test: an earlier version of {@code getCriticalResult()} checked only "two
     * dice show 1", so 1+1+5 (which has two 1s but doesn't sum to 4) was incorrectly read as
     * a Falha Crítica Menor. Falha Crítica Menor is specifically 1+1+2 (total 4) — anything
     * else with two 1s but a different third die is just a plain failure.
     */
    @Test
    void twoOnesWithAThirdDieOtherThanTwoIsNotCritical() {
        assertEquals(CriticalResult.NONE, new SkillRoll(List.of(1, 1, 3)).getCriticalResult());
        assertEquals(CriticalResult.NONE, new SkillRoll(List.of(1, 1, 4)).getCriticalResult());
        assertEquals(CriticalResult.NONE, new SkillRoll(List.of(1, 1, 5)).getCriticalResult());
        assertEquals(CriticalResult.NONE, new SkillRoll(List.of(1, 1, 6)).getCriticalResult());
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

    /**
     * Documents the known TODO on {@code getCriticalResult()}: unlike Falha Crítica Menor
     * (just fixed to require exactly 1+1+2), Acerto Crítico Menor is deliberately *not* fixed
     * to a specific third die yet, since its margin isn't fixed at "5" the way Falha Crítica
     * Menor's is fixed at "2" — abilities are expected to widen it. So two 6s plus *any*
     * third die still reads as Acerto Crítico Menor for now, matching this same class's
     * pre-fix behavior on the failure side — update this test if/when the margin-widening
     * mechanism lands and this gets a real fix.
     */
    @Test
    void twoSixesWithAnyThirdDieIsStillAcertoCriticoMenorForNow() {
        assertEquals(CriticalResult.ACERTO_CRITICO_MENOR, new SkillRoll(List.of(6, 6, 1)).getCriticalResult());
        assertEquals(CriticalResult.ACERTO_CRITICO_MENOR, new SkillRoll(List.of(6, 6, 3)).getCriticalResult());
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

    @Test
    void requestedAbilityDefaultsToNullWithTheSingleArgConstructor() {
        SkillRoll roll = new SkillRoll(List.of(2, 3, 5));

        assertNull(roll.getRequestedAbility());
    }

    @Test
    void requestedAbilityIsStoredWhenSupplied() {
        SkillRoll roll = new SkillRoll(List.of(2, 3, 5), ArtesCompetencyAbility.DOM_BARDICO);

        assertEquals(ArtesCompetencyAbility.DOM_BARDICO, roll.getRequestedAbility());
    }

    @Test
    void theTwoArgConstructorStillValidatesDice() {
        assertThrows(IllegalOperationException.class,
                () -> new SkillRoll(List.of(1, 2), ArtesCompetencyAbility.DOM_BARDICO));
    }

    @Test
    void requestedAbilityAcceptsASkillSpecializationToo() {
        SkillRoll roll = new SkillRoll(List.of(2, 3, 5), AttentionSpecialization.INVESTIGAR);

        assertEquals(AttentionSpecialization.INVESTIGAR, roll.getRequestedAbility());
    }
}
