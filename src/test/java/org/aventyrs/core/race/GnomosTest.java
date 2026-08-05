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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GnomosTest {

    private final Gnomos gnomos = new Gnomos();

    @Test
    void generateEmptyCharacterReturnsAUsableBuilder() {
        assertNotNull(gnomos.generateEmptyCharacter(List.of()));
    }

    @Test
    void generateEmptyCharacterSeedsMinusOneSizeCategory() {
        Character character = gnomos.generateEmptyCharacter(List.of())
                .player(new Player())
                .name("Test")
                .race(gnomos)
                .attributes(CharacterAttributes.builder().build())
                .egos(CharacterEgos.builder().build())
                .actionProfile(ActionProfile.REFLEXOS_RAPIDOS)
                .build();

        assertEquals(SizeCategory.MINUS_ONE, character.getSizeCategory());
    }

    @Test
    void hasAFixedCharismaRacialBonus() {
        assertEquals(Map.of(AttributeDomain.CHARISMA, 1), gnomos.getFixedAttributeBonuses());
    }

    @Test
    void hasOneChoosableBonusPointBetweenDexterityAndFocus() {
        assertEquals(1, gnomos.getChoosableAttributeBonusPoints());
        assertEquals(Set.of(AttributeDomain.DEXTERITY, AttributeDomain.FOCUS), gnomos.getChoosableAttributes());
    }

    @Test
    void hasNoRacialAbilities() {
        assertTrue(gnomos.getRacialAbilities().isEmpty());
    }

    @Test
    void usesTheBaseCostsForFeatsAndSkills() {
        assertEquals(Race.BASE_NEW_FEAT_COST, gnomos.getNewFeatCost(FeatCategory.PERITO));
        assertEquals(Race.BASE_NEW_SKILL_COST, gnomos.getNewSkillCost());
    }
}
