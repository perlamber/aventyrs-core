package org.aventyrs.core.item;

import org.aventyrs.core.skill.DifficultyLevel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ItemRarityTest {

    @Test
    void fabricationDifficultyClimbsOneTierPerRarityStep() {
        assertEquals(DifficultyLevel.MEDIUM, ItemRarity.COMMON.getFabricationDifficulty());
        assertEquals(DifficultyLevel.HARD, ItemRarity.UNCOMMON.getFabricationDifficulty());
        assertEquals(DifficultyLevel.VERY_HARD, ItemRarity.RARE.getFabricationDifficulty());
        assertEquals(DifficultyLevel.UNLIKELY, ItemRarity.EPIC.getFabricationDifficulty());
        assertEquals(DifficultyLevel.UNIMAGINABLE, ItemRarity.MYTHIC.getFabricationDifficulty());
    }

    @Test
    void improvementInstallDifficultyIsOneTierHarderThanFabrication() {
        assertEquals(DifficultyLevel.HARD, ItemRarity.COMMON.getImprovementInstallDifficulty());
        assertEquals(DifficultyLevel.VERY_HARD, ItemRarity.UNCOMMON.getImprovementInstallDifficulty());
        assertEquals(DifficultyLevel.UNLIKELY, ItemRarity.RARE.getImprovementInstallDifficulty());
        assertEquals(DifficultyLevel.UNIMAGINABLE, ItemRarity.EPIC.getImprovementInstallDifficulty());
        assertEquals(DifficultyLevel.MIRACLE, ItemRarity.MYTHIC.getImprovementInstallDifficulty());
    }

    @Test
    void ordinaryRepairIsEasierThanObraPrimaRepair() {
        assertEquals(DifficultyLevel.VERY_EASY, ItemRarity.COMMON.getRepairDifficulty());
        assertEquals(DifficultyLevel.VERY_HARD, ItemRarity.MYTHIC.getRepairDifficulty());

        assertEquals(DifficultyLevel.EASY, ItemRarity.COMMON.getMasterpieceRepairDifficulty());
        assertEquals(DifficultyLevel.HARD, ItemRarity.RARE.getMasterpieceRepairDifficulty());
        assertEquals(DifficultyLevel.UNLIKELY, ItemRarity.MYTHIC.getMasterpieceRepairDifficulty());
    }

    @Test
    void masterpieceGraduationFloorRisesWithRarity() {
        assertEquals(1, ItemRarity.COMMON.getMinimumMasterpieceGraduation());
        assertEquals(3, ItemRarity.UNCOMMON.getMinimumMasterpieceGraduation());
        assertEquals(5, ItemRarity.RARE.getMinimumMasterpieceGraduation());
        assertEquals(7, ItemRarity.EPIC.getMinimumMasterpieceGraduation());
        assertEquals(10, ItemRarity.MYTHIC.getMinimumMasterpieceGraduation());
    }

    @Test
    void naturalEquipmentIsNeverFabricatedOrRepaired() {
        assertThrows(IllegalStateException.class, ItemRarity.NATURAL::getFabricationDifficulty);
        assertThrows(IllegalStateException.class, ItemRarity.NATURAL::getRepairDifficulty);
        assertThrows(IllegalStateException.class, ItemRarity.NATURAL::getMasterpieceRepairDifficulty);
        assertThrows(IllegalStateException.class, ItemRarity.NATURAL::getImprovementInstallDifficulty);
        assertThrows(IllegalStateException.class, ItemRarity.NATURAL::getMinimumMasterpieceGraduation);
    }
}
