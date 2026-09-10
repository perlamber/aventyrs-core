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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AvianoTest {

    private final Aviano rapinante = new Aviano(Aviano.Subtipo.RAPINANTE);
    private final Aviano correnuvens = new Aviano(Aviano.Subtipo.CORRENUVENS);

    @Test
    void generateEmptyCharacterReturnsAUsableBuilder() {
        assertNotNull(rapinante.generateEmptyCharacter(List.of()));
    }

    @Test
    void generateEmptyCharacterLeavesTheDefaultZeroSizeCategory() {
        Character character = rapinante.generateEmptyCharacter(List.of())
                .player(new Player())
                .name("Test")
                .race(rapinante)
                .attributes(CharacterAttributes.builder().build())
                .egos(CharacterEgos.builder().build())
                .actionProfile(ActionProfile.REFLEXOS_RAPIDOS)
                .build();

        assertEquals(SizeCategory.ZERO, character.getSizeCategory());
    }

    @Test
    void hasMonstruosoCreatureTypeAndZeroBaseSizeCategory() {
        assertEquals(CreatureType.MONSTRUOSO, rapinante.getCreatureType());
        assertEquals(SizeCategory.ZERO, rapinante.getBaseSizeCategory());
    }

    @Test
    void aRapinanteHasFixedDexterityAndStrengthRacialBonuses() {
        assertEquals(Map.of(AttributeDomain.DEXTERITY, 1, AttributeDomain.STRENGTH, 1),
                rapinante.getFixedAttributeBonuses());
    }

    @Test
    void aCorrenuvensHasFixedDexterityAndVigorRacialBonuses() {
        assertEquals(Map.of(AttributeDomain.DEXTERITY, 1, AttributeDomain.VIGOR, 1),
                correnuvens.getFixedAttributeBonuses());
    }

    @Test
    void hasNoChoosableRacialBonuses() {
        assertEquals(0, rapinante.getChoosableAttributeBonusPoints());
        assertTrue(rapinante.getChoosableAttributes().isEmpty());
    }

    @Test
    void usesTheBaseCostsForFeatsAndSkills() {
        assertEquals(Race.BASE_NEW_FEAT_COST, rapinante.getNewFeatCost(FeatCategory.AVIANO));
        assertEquals(Race.BASE_NEW_SKILL_COST, rapinante.getNewSkillCost());
    }

    @Test
    void isNotMestico() {
        assertFalse(rapinante.isMestico());
    }

    @Test
    void grantsVisaoAlemDoAlcanceAsARacialAbility() {
        assertEquals(List.of(AvianosRacialAbility.VISAO_ALEM_DO_ALCANCE), rapinante.getRacialAbilities());
        assertEquals(List.of(AvianosRacialAbility.VISAO_ALEM_DO_ALCANCE), correnuvens.getRacialAbilities());
    }

    @Test
    void grantsNoCriticalEffectImmunities() {
        assertTrue(rapinante.getCriticalEffectImmunities().isEmpty());
    }
}
