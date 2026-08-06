package org.aventyrs.core.race;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.feat.FeatCategory;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GorgonaTest {

    private final Gorgona gorgona = new Gorgona();

    @Test
    void generateEmptyCharacterReturnsAUsableBuilder() {
        assertNotNull(gorgona.generateEmptyCharacter(List.of()));
    }

    @Test
    void hasFixedStrengthAndCharismaRacialBonuses() {
        assertEquals(Map.of(AttributeDomain.STRENGTH, 1, AttributeDomain.CHARISMA, 1), gorgona.getFixedAttributeBonuses());
    }

    @Test
    void hasNoChoosableRacialBonus() {
        assertEquals(0, gorgona.getChoosableAttributeBonusPoints());
        assertTrue(gorgona.getChoosableAttributes().isEmpty());
    }

    @Test
    void hasNoRacialAbilitiesByDefault() {
        assertTrue(gorgona.getRacialAbilities().isEmpty());
    }

    @Test
    void usesTheBaseCostsForFeatsAndSkills() {
        assertEquals(Race.BASE_NEW_FEAT_COST, gorgona.getNewFeatCost(FeatCategory.MONSTRUOSO));
        assertEquals(Race.BASE_NEW_SKILL_COST, gorgona.getNewSkillCost());
    }
}
