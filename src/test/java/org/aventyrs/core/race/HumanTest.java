package org.aventyrs.core.race;

import org.aventyrs.core.feat.FeatCategory;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HumanTest {

    private final Human human = new Human();

    @Test
    void generateEmptyCharacterReturnsAUsableBuilder() {
        assertNotNull(human.generateEmptyCharacter(List.of()));
    }

    @Test
    void hasNoFixedOrChoosableRacialBonusesByDefault() {
        assertTrue(human.getFixedAttributeBonuses().isEmpty());
        assertEquals(0, human.getChoosableAttributeBonusPoints());
        assertTrue(human.getChoosableAttributes().isEmpty());
    }

    @Test
    void hasNoRacialAbilitiesByDefault() {
        assertTrue(human.getRacialAbilities().isEmpty());
    }

    @Test
    void usesTheBaseCostsForFeatsAndSkills() {
        assertEquals(Race.BASE_NEW_FEAT_COST, human.getNewFeatCost(FeatCategory.DUELISTA));
        assertEquals(Race.BASE_NEW_SKILL_COST, human.getNewSkillCost());
    }
}
