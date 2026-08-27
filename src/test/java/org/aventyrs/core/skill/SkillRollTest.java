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
     * Unlike Falha Crítica Menor (fixed to require exactly 1+1+2), Acerto Crítico Menor is
     * deliberately *not* fixed to one specific third die, even with no margin widening applied
     * (see {@link CriticalResult}'s own javadoc) — any two 6s is enough regardless of the third
     * die, so the margin-widening mechanism (see {@link #getCriticalResultWidensAcertoCriticoMenorByTheGivenMargin})
     * has something consistent to widen from.
     */
    @Test
    void twoSixesWithAnyThirdDieIsAcertoCriticoMenor() {
        assertEquals(CriticalResult.ACERTO_CRITICO_MENOR, new SkillRoll(List.of(6, 6, 1)).getCriticalResult());
        assertEquals(CriticalResult.ACERTO_CRITICO_MENOR, new SkillRoll(List.of(6, 6, 3)).getCriticalResult());
    }

    /**
     * A margin of 1 (e.g. {@code AtaqueCorpoACorpoCompetencyAbility#ATAQUE_PRECISO}'s own "5s
     * counting alongside 6s" rules text) lowers the qualifying face from 6 to 5 — two 5s now
     * also read as Acerto Crítico Menor, which they don't at margin 0.
     */
    @Test
    void getCriticalResultWidensAcertoCriticoMenorByTheGivenMargin() {
        SkillRoll twoFives = new SkillRoll(List.of(5, 5, 2));

        assertEquals(CriticalResult.NONE, twoFives.getCriticalResult(0));
        assertEquals(CriticalResult.ACERTO_CRITICO_MENOR, twoFives.getCriticalResult(1));
    }

    /** A mixed pair (one 5, one 6) also qualifies once the margin widens the threshold to 5. */
    @Test
    void getCriticalResultCountsAMixedPairOnceWidened() {
        SkillRoll fiveAndSix = new SkillRoll(List.of(5, 6, 2));

        assertEquals(CriticalResult.NONE, fiveAndSix.getCriticalResult(0));
        assertEquals(CriticalResult.ACERTO_CRITICO_MENOR, fiveAndSix.getCriticalResult(1));
    }

    /** A negative margin (never expected from a real source) is treated the same as 0. */
    @Test
    void getCriticalResultTreatsANegativeMarginAsZero() {
        SkillRoll twoFives = new SkillRoll(List.of(5, 5, 2));

        assertEquals(CriticalResult.NONE, twoFives.getCriticalResult(-1));
    }

    /**
     * The margin only ever widens Acerto Crítico Menor — Falha Crítica Maior/Menor and Acerto
     * Crítico Maior are each fixed at one exact dice combination in this ruleset's own rules
     * text, with no ability anywhere citing a margin on any of the three.
     */
    @Test
    void getCriticalResultMarginDoesNotAffectTheOtherThreeOutcomes() {
        assertEquals(CriticalResult.FALHA_CRITICA_MAIOR, new SkillRoll(List.of(1, 1, 1)).getCriticalResult(5));
        assertEquals(CriticalResult.FALHA_CRITICA_MENOR, new SkillRoll(List.of(1, 1, 2)).getCriticalResult(5));
        assertEquals(CriticalResult.ACERTO_CRITICO_MAIOR, new SkillRoll(List.of(6, 6, 6)).getCriticalResult(5));
    }

    /** {@link SkillRoll#getCriticalResult()} is exactly {@code getCriticalResult(0)}. */
    @Test
    void noArgGetCriticalResultAppliesNoMargin() {
        SkillRoll twoFives = new SkillRoll(List.of(5, 5, 2));

        assertEquals(twoFives.getCriticalResult(0), twoFives.getCriticalResult());
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
