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

class PequeninoTest {

    private final Pequenino pequenino = new Pequenino();

    @Test
    void generateEmptyCharacterReturnsAUsableBuilder() {
        assertNotNull(pequenino.generateEmptyCharacter(List.of()));
    }

    @Test
    void generateEmptyCharacterSeedsMinusOneSizeCategory() {
        Character character = pequenino.generateEmptyCharacter(List.of())
                .player(new Player())
                .name("Test")
                .race(pequenino)
                .attributes(CharacterAttributes.builder().build())
                .egos(CharacterEgos.builder().build())
                .actionProfile(ActionProfile.REFLEXOS_RAPIDOS)
                .build();

        assertEquals(SizeCategory.MINUS_ONE, character.getSizeCategory());
    }

    @Test
    void hasAFixedDexterityRacialBonus() {
        assertEquals(Map.of(AttributeDomain.DEXTERITY, 1), pequenino.getFixedAttributeBonuses());
    }

    @Test
    void hasNoChoosableRacialBonuses() {
        assertEquals(0, pequenino.getChoosableAttributeBonusPoints());
        assertTrue(pequenino.getChoosableAttributes().isEmpty());
    }

    @Test
    void hasNoRacialAbilities() {
        assertTrue(pequenino.getRacialAbilities().isEmpty());
    }

    @Test
    void usesTheBaseCostsForFeatsAndSkills() {
        assertEquals(Race.BASE_NEW_FEAT_COST, pequenino.getNewFeatCost(FeatCategory.MOBILIDADE));
        assertEquals(Race.BASE_NEW_SKILL_COST, pequenino.getNewSkillCost());
    }
}
