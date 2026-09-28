package org.aventyrs.core.monster;

import org.aventyrs.core.skill.DifficultyLevel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MonsterCategoryTest {

    @Test
    void eachBoundaryOfTheGrauDePoderTableLandsOnItsCategoria() {
        assertEquals(MonsterCategory.PRESA, MonsterCategory.forPowerDegree(0));
        assertEquals(MonsterCategory.PRESA, MonsterCategory.forPowerDegree(11));
        assertEquals(MonsterCategory.DEVIANTE, MonsterCategory.forPowerDegree(12));
        assertEquals(MonsterCategory.DEVIANTE, MonsterCategory.forPowerDegree(25));
        assertEquals(MonsterCategory.PREDADOR, MonsterCategory.forPowerDegree(26));
        assertEquals(MonsterCategory.PREDADOR, MonsterCategory.forPowerDegree(45));
        assertEquals(MonsterCategory.APEX, MonsterCategory.forPowerDegree(46));
        assertEquals(MonsterCategory.APEX, MonsterCategory.forPowerDegree(60));
        assertEquals(MonsterCategory.ABOMINACAO, MonsterCategory.forPowerDegree(61));
        assertEquals(MonsterCategory.ABOMINACAO, MonsterCategory.forPowerDegree(500));
    }

    @Test
    void aNegativeGrauDePoderIsAPresa() {
        assertEquals(MonsterCategory.PRESA, MonsterCategory.forPowerDegree(-3));
    }

    @Test
    void theTableColumnsMatchTheRules() {
        assertEquals(DifficultyLevel.HARD, MonsterCategory.PRESA.getMaximumSkillLevel());
        assertEquals(DifficultyLevel.VERY_HARD, MonsterCategory.DEVIANTE.getMaximumSkillLevel());
        assertEquals(DifficultyLevel.UNLIKELY, MonsterCategory.PREDADOR.getMaximumSkillLevel());
        assertEquals(DifficultyLevel.UNIMAGINABLE, MonsterCategory.APEX.getMaximumSkillLevel());
        assertEquals(DifficultyLevel.MIRACLE, MonsterCategory.ABOMINACAO.getMaximumSkillLevel());

        int[] attributes = {6, 7, 8, 9, 10};
        int[] multipliers = {5, 6, 8, 10, 12};
        int[] models = {3, 4, 5, 6, 8};
        int[] actionPoints = {0, 0, 1, 2, 3};
        int[] egoPoints = {0, 1, 3, 6, 11};
        for (MonsterCategory category : MonsterCategory.values()) {
            int i = category.ordinal();
            assertEquals(attributes[i], category.getMaximumAttribute(), category.name());
            assertEquals(multipliers[i], category.getLifeMultiplier(), category.name());
            assertEquals(models[i], category.getMaximumModels(), category.name());
            assertEquals(actionPoints[i], category.getBonusActionPoints(), category.name());
            assertEquals(egoPoints[i], category.getBonusEgoPoints(), category.name());
        }
    }

    @Test
    void isAtLeastFollowsThePowerOrder() {
        assertTrue(MonsterCategory.PREDADOR.isAtLeast(MonsterCategory.DEVIANTE));
        assertTrue(MonsterCategory.PREDADOR.isAtLeast(MonsterCategory.PREDADOR));
        assertFalse(MonsterCategory.PREDADOR.isAtLeast(MonsterCategory.APEX));
    }
}
