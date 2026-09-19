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

    /** 17 — the default Margem Crítica Menor exactly — is an Acerto Crítico Menor. */
    @Test
    void reachingTheDefaultMarginIsAcertoCriticoMenor() {
        SkillRoll roll = new SkillRoll(List.of(6, 5, 6));

        assertEquals(CriticalResult.ACERTO_CRITICO_MENOR, roll.getCriticalResult());
    }

    /**
     * Acerto Crítico Menor is a <em>total</em>, not a pair of matching faces: the Margem Crítica
     * Menor an Arma prints in parentheses is the 3d6 sum to reach (see {@link CriticalResult}), so
     * two 6s alongside a low third die falls short of the default 17 and is an ordinary hit. This
     * is what the pre-0.0.41 "two dice showing 6" approximation got wrong.
     */
    @Test
    void twoSixesBelowTheMarginIsNotCritical() {
        assertEquals(CriticalResult.NONE, new SkillRoll(List.of(6, 6, 1)).getCriticalResult());
        assertEquals(CriticalResult.NONE, new SkillRoll(List.of(6, 6, 3)).getCriticalResult());
    }

    /**
     * A margin of 1 (e.g. {@code AtaqueCorpoACorpoCompetencyAbility#ATAQUE_PRECISO}'s "+1 número")
     * lowers the total to reach from 17 to 16 — a 16 now reads as Acerto Crítico Menor, which it
     * doesn't at margin 0.
     */
    @Test
    void getCriticalResultWidensAcertoCriticoMenorByTheGivenMargin() {
        SkillRoll sixteen = new SkillRoll(List.of(6, 6, 4));

        assertEquals(CriticalResult.NONE, sixteen.getCriticalResult(0));
        assertEquals(CriticalResult.ACERTO_CRITICO_MENOR, sixteen.getCriticalResult(1));
    }

    /**
     * A weapon's own margin is what the roll is judged against — a Florete's authored 16 crits on a
     * total of 16, with no widening held at all.
     */
    @Test
    void getCriticalResultReadsTheWeaponsOwnMargin() {
        SkillRoll sixteen = new SkillRoll(List.of(6, 6, 4));

        assertEquals(CriticalResult.NONE, sixteen.getCriticalResult(0, 17));
        assertEquals(CriticalResult.ACERTO_CRITICO_MENOR, sixteen.getCriticalResult(0, 16));
    }

    /** Widening is floored so it can never turn a Falha Crítica into a success. */
    @Test
    void absurdWideningStillLeavesTheFalhasAlone() {
        assertEquals(CriticalResult.FALHA_CRITICA_MAIOR, new SkillRoll(List.of(1, 1, 1)).getCriticalResult(50));
        assertEquals(CriticalResult.FALHA_CRITICA_MENOR, new SkillRoll(List.of(1, 1, 2)).getCriticalResult(50));
        assertEquals(CriticalResult.ACERTO_CRITICO_MENOR, new SkillRoll(List.of(1, 1, 3)).getCriticalResult(50));
    }

    /** A negative margin (never expected from a real source) is treated the same as 0. */
    @Test
    void getCriticalResultTreatsANegativeMarginAsZero() {
        SkillRoll sixteen = new SkillRoll(List.of(6, 6, 4));

        assertEquals(CriticalResult.NONE, sixteen.getCriticalResult(-1));
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
        SkillRoll sixteen = new SkillRoll(List.of(6, 6, 4));

        assertEquals(sixteen.getCriticalResult(0), sixteen.getCriticalResult());
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
