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
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnoesTest {

    private final Anoes anoes = new Anoes();

    @Test
    void generateEmptyCharacterSeedsMinusOneSizeCategory() {
        Character character = anoes.generateEmptyCharacter(List.of())
                .player(new Player())
                .name("Test")
                .race(anoes)
                .attributes(CharacterAttributes.builder().build())
                .egos(CharacterEgos.builder().build())
                .actionProfile(ActionProfile.REFLEXOS_RAPIDOS)
                .build();

        assertEquals(SizeCategory.MINUS_ONE, character.getSizeCategory());
    }

    @Test
    void hasFixedVigorAndGnoseRacialBonuses() {
        assertEquals(Map.of(AttributeDomain.VIGOR, 1, AttributeDomain.GNOSE, 1), anoes.getFixedAttributeBonuses());
    }

    @Test
    void hasNoChoosableRacialBonuses() {
        assertEquals(0, anoes.getChoosableAttributeBonusPoints());
        assertTrue(anoes.getChoosableAttributes().isEmpty());
    }

    @Test
    void usesTheBaseCostsForFeatsAndSkills() {
        assertEquals(Race.BASE_NEW_FEAT_COST, anoes.getNewFeatCost(FeatCategory.SOBREVIVENCIA));
        assertEquals(Race.BASE_NEW_SKILL_COST, anoes.getNewSkillCost());
    }

    @Test
    void grantsAbatedoresDeGigantesAsARacialAbility() {
        assertEquals(List.of(AnoesRacialAbility.ABATEDORES_DE_GIGANTES), anoes.getRacialAbilities());
    }
}
