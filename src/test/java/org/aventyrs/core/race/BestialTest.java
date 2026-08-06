package org.aventyrs.core.race;

import org.aventyrs.core.feat.FeatCategory;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BestialTest {

    private final Bestial bestial = new Bestial();

    @Test
    void generateEmptyCharacterReturnsAUsableBuilder() {
        assertNotNull(bestial.generateEmptyCharacter(List.of()));
    }

    @Test
    void hasNoFixedOrChoosableRacialBonusesByDefault() {
        assertTrue(bestial.getFixedAttributeBonuses().isEmpty());
        assertEquals(0, bestial.getChoosableAttributeBonusPoints());
        assertTrue(bestial.getChoosableAttributes().isEmpty());
    }

    @Test
    void hasNoRacialAbilitiesByDefault() {
        assertTrue(bestial.getRacialAbilities().isEmpty());
    }

    @Test
    void usesTheBaseCostsForFeatsAndSkills() {
        assertEquals(Race.BASE_NEW_FEAT_COST, bestial.getNewFeatCost(FeatCategory.BESTIAL));
        assertEquals(Race.BASE_NEW_SKILL_COST, bestial.getNewSkillCost());
    }
}
