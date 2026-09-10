package org.aventyrs.core.race;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.feat.FeatCategory;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FadaTest {

    private final Fada fada = new Fada();

    @Test
    void generateEmptyCharacterReturnsAUsableBuilder() {
        assertNotNull(fada.generateEmptyCharacter(List.of()));
    }

    @Test
    void hasFeericoCreatureType() {
        assertEquals(CreatureType.FEERICO, fada.getCreatureType());
    }

    @Test
    void hasFixedCharismaAndFocusRacialBonuses() {
        assertEquals(Map.of(AttributeDomain.CHARISMA, 1, AttributeDomain.FOCUS, 1), fada.getFixedAttributeBonuses());
    }

    @Test
    void hasNoChoosableRacialBonus() {
        assertEquals(0, fada.getChoosableAttributeBonusPoints());
        assertTrue(fada.getChoosableAttributes().isEmpty());
    }

    @Test
    void hasNoRacialAbilitiesByDefault() {
        assertTrue(fada.getRacialAbilities().isEmpty());
    }

    @Test
    void usesTheBaseCostsForFeatsAndSkills() {
        assertEquals(Race.BASE_NEW_FEAT_COST, fada.getNewFeatCost(FeatCategory.FEERICO));
        assertEquals(Race.BASE_NEW_SKILL_COST, fada.getNewSkillCost());
    }
}
