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

class GigantesTest {

    private final Gigantes gigantes = new Gigantes();

    @Test
    void generateEmptyCharacterReturnsAUsableBuilder() {
        assertNotNull(gigantes.generateEmptyCharacter(List.of()));
    }

    @Test
    void generateEmptyCharacterSeedsPlusTwoSizeCategory() {
        Character character = gigantes.generateEmptyCharacter(List.of())
                .player(new Player())
                .name("Test")
                .race(gigantes)
                .attributes(CharacterAttributes.builder().build())
                .egos(CharacterEgos.builder().build())
                .actionProfile(ActionProfile.REFLEXOS_RAPIDOS)
                .build();

        assertEquals(SizeCategory.PLUS_TWO, character.getSizeCategory());
    }

    @Test
    void hasFixedStrengthAndVigorRacialBonuses() {
        assertEquals(Map.of(AttributeDomain.STRENGTH, 2, AttributeDomain.VIGOR, 2), gigantes.getFixedAttributeBonuses());
    }

    @Test
    void hasNoChoosableRacialBonuses() {
        assertEquals(0, gigantes.getChoosableAttributeBonusPoints());
        assertTrue(gigantes.getChoosableAttributes().isEmpty());
    }

    @Test
    void hasNoRacialAbilities() {
        assertTrue(gigantes.getRacialAbilities().isEmpty());
    }

    @Test
    void sobrevivenciaFeatsCostLessThanTheBaseCost() {
        assertEquals(2, gigantes.getNewFeatCost(FeatCategory.SOBREVIVENCIA));
    }

    @Test
    void everyOtherFeatCategoryUsesTheBaseCost() {
        for (FeatCategory featCategory : FeatCategory.values()) {
            if (featCategory != FeatCategory.SOBREVIVENCIA) {
                assertEquals(Race.BASE_NEW_FEAT_COST, gigantes.getNewFeatCost(featCategory));
            }
        }
        assertEquals(Race.BASE_NEW_SKILL_COST, gigantes.getNewSkillCost());
    }
}
