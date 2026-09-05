package org.aventyrs.core.item;

import org.aventyrs.core.skill.DifficultyLevel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegaliaGradeTest {

    @Test
    void carriesTheGdDaysAndDonorRulesOfEachGrade() {
        assertEquals(DifficultyLevel.UNIMAGINABLE, RegaliaGrade.MENOR.getCraftingDifficulty());
        assertEquals(DifficultyLevel.MIRACLE, RegaliaGrade.SUPERIOR.getCraftingDifficulty());
        assertEquals(DifficultyLevel.MIRACLE, RegaliaGrade.DIVINA.getCraftingDifficulty());

        assertEquals(90, RegaliaGrade.MENOR.getCraftingTimeInDays());
        assertEquals(145, RegaliaGrade.SUPERIOR.getCraftingTimeInDays());
        assertEquals(180, RegaliaGrade.DIVINA.getCraftingTimeInDays());
    }

    @Test
    void onlyDivinaDemandsACriticalResultAndAnExternalDonor() {
        assertFalse(RegaliaGrade.MENOR.requiresCriticalResult());
        assertFalse(RegaliaGrade.SUPERIOR.requiresCriticalResult());
        assertTrue(RegaliaGrade.DIVINA.requiresCriticalResult());

        assertFalse(RegaliaGrade.MENOR.requiresExternalDonor());
        assertFalse(RegaliaGrade.SUPERIOR.requiresExternalDonor());
        assertTrue(RegaliaGrade.DIVINA.requiresExternalDonor());
    }

    @Test
    void onlySuperiorAndDivinaTakeAllTheDonorsCentelhas() {
        assertFalse(RegaliaGrade.MENOR.requiresAllCentelhas());
        assertTrue(RegaliaGrade.SUPERIOR.requiresAllCentelhas());
        assertTrue(RegaliaGrade.DIVINA.requiresAllCentelhas());
    }

    @Test
    void gradesRankFromMenorToDivinaSoAHigherGradeSatisfiesALowerPossessionGate() {
        assertTrue(RegaliaGrade.SUPERIOR.compareTo(RegaliaGrade.MENOR) > 0);
        assertTrue(RegaliaGrade.DIVINA.compareTo(RegaliaGrade.SUPERIOR) > 0);
    }

    @Test
    void isRegaliaFollowsTheGradeMarkerOnAForgedCopy() {
        AbstractItem ordinary = AbstractItem.builder().name("Espada").category(ItemCategory.HEAVY_BLADE).build();
        AbstractItem regalia = AbstractItem.builder().name("Excalibur").category(ItemCategory.HEAVY_BLADE)
                .regaliaGrade(RegaliaGrade.SUPERIOR).build();

        assertFalse(ordinary.isRegalia());
        assertTrue(regalia.isRegalia());
        assertEquals(RegaliaGrade.SUPERIOR, regalia.getRegaliaGrade());
    }
}
