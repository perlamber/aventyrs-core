package org.aventyrs.core.race;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.feat.FeatCategory;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FuriaTest {

    private final Furia furia = new Furia();

    @Test
    void generateEmptyCharacterReturnsAUsableBuilder() {
        assertNotNull(furia.generateEmptyCharacter(List.of()));
    }

    @Test
    void hasFixedCharismaAndFocusRacialBonuses() {
        assertEquals(Map.of(AttributeDomain.CHARISMA, 1, AttributeDomain.FOCUS, 1), furia.getFixedAttributeBonuses());
    }

    @Test
    void hasNoChoosableRacialBonus() {
        assertEquals(0, furia.getChoosableAttributeBonusPoints());
        assertTrue(furia.getChoosableAttributes().isEmpty());
    }

    @Test
    void hasNoRacialAbilitiesByDefault() {
        assertTrue(furia.getRacialAbilities().isEmpty());
    }

    @Test
    void usesTheBaseCostsForFeatsAndSkills() {
        assertEquals(Race.BASE_NEW_FEAT_COST, furia.getNewFeatCost(FeatCategory.FERRICO));
        assertEquals(Race.BASE_NEW_SKILL_COST, furia.getNewSkillCost());
    }
}
