package org.aventyrs.core.race;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.feat.FeatCategory;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrcsTest {

    private final Orc orcs = new Orc();

    @Test
    void generateEmptyCharacterReturnsAUsableBuilder() {
        assertNotNull(orcs.generateEmptyCharacter(List.of()));
    }

    @Test
    void hasAFixedVigorRacialBonus() {
        assertEquals(Map.of(AttributeDomain.VIGOR, 2), orcs.getFixedAttributeBonuses());
    }

    @Test
    void hasNoChoosableRacialBonuses() {
        assertEquals(0, orcs.getChoosableAttributeBonusPoints());
        assertTrue(orcs.getChoosableAttributes().isEmpty());
    }

    @Test
    void hasNoRacialAbilities() {
        assertTrue(orcs.getRacialAbilities().isEmpty());
    }

    @Test
    void usesTheBaseCostsForFeatsAndSkills() {
        assertEquals(Race.BASE_NEW_FEAT_COST, orcs.getNewFeatCost(FeatCategory.SOBREVIVENCIA));
        assertEquals(Race.BASE_NEW_SKILL_COST, orcs.getNewSkillCost());
    }
}
