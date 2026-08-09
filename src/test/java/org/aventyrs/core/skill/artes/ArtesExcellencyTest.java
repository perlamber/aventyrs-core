package org.aventyrs.core.skill.artes;

import org.aventyrs.core.skill.DifficultyLevel;
import org.aventyrs.core.skill.SkillExcellency;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ArtesExcellencyTest {

    @Test
    void everyExcellencyBelongsToArtes() {
        for (ArtesExcellency excellency : ArtesExcellency.values()) {
            assertEquals(SkillType.ARTES, excellency.getSkillType());
        }
    }

    @Test
    void everyExcellencyHasADescription() {
        for (ArtesExcellency excellency : ArtesExcellency.values()) {
            assertFalse(excellency.getDescription().isBlank());
        }
    }

    @Test
    void listHasTheThreeUniversalTiers() {
        assertEquals(3, ArtesExcellency.values().length);
    }

    @Test
    void onlyProdigioAdjustsDifficulty() {
        for (ArtesExcellency excellency : ArtesExcellency.values()) {
            DifficultyLevel expected = excellency == ArtesExcellency.PRODIGIO
                    ? DifficultyLevel.EASY
                    : DifficultyLevel.MEDIUM;
            assertEquals(expected, excellency.adjustDifficulty(DifficultyLevel.MEDIUM));
        }
    }

    @Test
    void unlockedByFiltersArtesExcellencyByGraduation() {
        assertEquals(List.of(), SkillExcellency.unlockedBy(ArtesExcellency.class, 2));
        assertEquals(List.of(ArtesExcellency.FOCADO), SkillExcellency.unlockedBy(ArtesExcellency.class, 5));
        assertEquals(List.of(ArtesExcellency.FOCADO, ArtesExcellency.PRODIGIO, ArtesExcellency.LENDA),
                SkillExcellency.unlockedBy(ArtesExcellency.class, 10));
    }

    @Test
    void onlyProdigioReducesDifficulty() {
        for (ArtesExcellency excellency : ArtesExcellency.values()) {
            int expected = excellency == ArtesExcellency.PRODIGIO ? 1 : 0;
            assertEquals(expected, excellency.getDifficultyReduction());
        }
    }

    @Test
    void totalDifficultyReductionIsZeroBeforeProdigioIsUnlocked() {
        assertEquals(0, SkillExcellency.totalDifficultyReduction(ArtesExcellency.class, 5));
    }

    @Test
    void totalDifficultyReductionCountsProdigioOnceUnlocked() {
        assertEquals(1, SkillExcellency.totalDifficultyReduction(ArtesExcellency.class, 7));
        assertEquals(1, SkillExcellency.totalDifficultyReduction(ArtesExcellency.class, 10));
    }
}
