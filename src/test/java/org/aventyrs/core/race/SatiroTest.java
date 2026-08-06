package org.aventyrs.core.race;

import org.aventyrs.core.action.ActionProfile;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterEgos;
import org.aventyrs.core.character.SizeCategory;
import org.aventyrs.core.feat.FeatCategory;
import org.aventyrs.core.sheet.Player;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SatiroTest {

    private final Satiro satiro = new Satiro();

    @Test
    void generateEmptyCharacterReturnsAUsableBuilder() {
        assertNotNull(satiro.generateEmptyCharacter(List.of()));
    }

    @Test
    void generateEmptyCharacterSeedsMinusOneSizeCategory() {
        Character character = satiro.generateEmptyCharacter(List.of())
                .player(new Player())
                .name("Test")
                .race(satiro)
                .attributes(CharacterAttributes.builder().build())
                .egos(CharacterEgos.builder().build())
                .actionProfile(ActionProfile.REFLEXOS_RAPIDOS)
                .build();

        assertEquals(SizeCategory.MINUS_ONE, character.getSizeCategory());
    }

    @Test
    void hasFixedInstinctAndCharismaRacialBonuses() {
        assertEquals(Map.of(AttributeDomain.INSTINCT, 1, AttributeDomain.CHARISMA, 1), satiro.getFixedAttributeBonuses());
    }

    @Test
    void hasNoChoosableRacialBonus() {
        assertEquals(0, satiro.getChoosableAttributeBonusPoints());
        assertTrue(satiro.getChoosableAttributes().isEmpty());
    }

    @Test
    void hasNoRacialAbilitiesByDefault() {
        assertTrue(satiro.getRacialAbilities().isEmpty());
    }

    @Test
    void usesTheBaseCostsForFeatsAndSkills() {
        assertEquals(Race.BASE_NEW_FEAT_COST, satiro.getNewFeatCost(FeatCategory.FERRICO));
        assertEquals(Race.BASE_NEW_SKILL_COST, satiro.getNewSkillCost());
    }
}
