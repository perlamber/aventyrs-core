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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OgroTest {

    private final Ogro forcaBruta = new Ogro(Ogro.Aptidao.FORCA_BRUTA);
    private final Ogro agilidadeBruta = new Ogro(Ogro.Aptidao.AGILIDADE_BRUTA);

    @Test
    void generateEmptyCharacterSeedsPlusOneSizeCategory() {
        Character character = forcaBruta.generateEmptyCharacter(List.of())
                .player(new Player())
                .name("Test")
                .race(forcaBruta)
                .attributes(CharacterAttributes.builder().build())
                .egos(CharacterEgos.builder().build())
                .actionProfile(ActionProfile.REFLEXOS_RAPIDOS)
                .build();

        assertEquals(SizeCategory.PLUS_ONE, character.getSizeCategory());
    }

    @Test
    void hasMonstruosoCreatureTypeAndPlusOneBaseSizeCategory() {
        assertEquals(CreatureType.MONSTRUOSO, forcaBruta.getCreatureType());
        assertEquals(SizeCategory.PLUS_ONE, forcaBruta.getBaseSizeCategory());
    }

    @Test
    void forcaBrutaGrantsTwoStrengthAndOneDexterity() {
        assertEquals(Map.of(AttributeDomain.STRENGTH, 2, AttributeDomain.DEXTERITY, 1),
                forcaBruta.getFixedAttributeBonuses());
    }

    @Test
    void agilidadeBrutaGrantsOneStrengthAndTwoDexterity() {
        assertEquals(Map.of(AttributeDomain.STRENGTH, 1, AttributeDomain.DEXTERITY, 2),
                agilidadeBruta.getFixedAttributeBonuses());
    }

    @Test
    void everyAptidaoAllocatesExactlyThreeRacialPoints() {
        for (Ogro.Aptidao aptidao : Ogro.Aptidao.values()) {
            assertEquals(3, aptidao.getAttributeBonuses().values().stream().mapToInt(Integer::intValue).sum());
        }
    }

    @Test
    void hasNoChoosableRacialBonuses() {
        assertEquals(0, forcaBruta.getChoosableAttributeBonusPoints());
        assertTrue(forcaBruta.getChoosableAttributes().isEmpty());
    }

    @Test
    void usesTheBaseCostsForFeatsAndSkills() {
        assertEquals(Race.BASE_NEW_FEAT_COST, forcaBruta.getNewFeatCost(FeatCategory.OGRICO));
        assertEquals(Race.BASE_NEW_SKILL_COST, forcaBruta.getNewSkillCost());
    }

    @Test
    void isNotMesticoAndGrantsNoRacialAbilities() {
        assertFalse(forcaBruta.isMestico());
        assertTrue(forcaBruta.getRacialAbilities().isEmpty());
        assertTrue(forcaBruta.getCriticalEffectImmunities().isEmpty());
    }
}
