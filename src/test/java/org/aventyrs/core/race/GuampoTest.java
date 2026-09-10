package org.aventyrs.core.race;

import org.aventyrs.core.action.ActionProfile;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterEgos;
import org.aventyrs.core.character.SizeCategory;
import org.aventyrs.core.character.services.HitPointsService;
import org.aventyrs.core.feat.FeatCategory;
import org.aventyrs.core.sheet.Player;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuampoTest {

    private final Guampo guampo = new Guampo();

    private Character buildCharacter() {
        return guampo.generateEmptyCharacter(List.of())
                .player(new Player())
                .name("Test")
                .race(guampo)
                .attributes(CharacterAttributes.builder().build())
                .egos(CharacterEgos.builder().build())
                .actionProfile(ActionProfile.REFLEXOS_RAPIDOS)
                .build();
    }

    @Test
    void generateEmptyCharacterSeedsPlusOneSizeCategory() {
        assertEquals(SizeCategory.PLUS_ONE, buildCharacter().getSizeCategory());
    }

    @Test
    void generateEmptyCharacterSeedsTheVigorDeEponaLifeMultiplier() {
        assertEquals(HitPointsService.DEFAULT_LIFE_MULTIPLIER + 1, buildCharacter().getLifeMultiplier());
    }

    @Test
    void hasMonstruosoCreatureTypeAndPlusOneBaseSizeCategory() {
        assertEquals(CreatureType.MONSTRUOSO, guampo.getCreatureType());
        assertEquals(SizeCategory.PLUS_ONE, guampo.getBaseSizeCategory());
    }

    @Test
    void hasFixedGnoseAndInstinctRacialBonuses() {
        assertEquals(Map.of(AttributeDomain.GNOSE, 1, AttributeDomain.INSTINCT, 1),
                guampo.getFixedAttributeBonuses());
    }

    @Test
    void hasNoChoosableRacialBonuses() {
        assertEquals(0, guampo.getChoosableAttributeBonusPoints());
        assertTrue(guampo.getChoosableAttributes().isEmpty());
    }

    @Test
    void usesTheBaseCostsForFeatsAndSkills() {
        assertEquals(Race.BASE_NEW_FEAT_COST, guampo.getNewFeatCost(FeatCategory.SOBREVIVENCIA));
        assertEquals(Race.BASE_NEW_SKILL_COST, guampo.getNewSkillCost());
    }

    @Test
    void isNotMestico() {
        assertFalse(guampo.isMestico());
    }

    @Test
    void grantsVigorDeEponaAsARacialAbility() {
        assertEquals(List.of(GuamposRacialAbility.VIGOR_DE_EPONA), guampo.getRacialAbilities());
    }

    @Test
    void grantsNoCriticalEffectImmunities() {
        assertTrue(guampo.getCriticalEffectImmunities().isEmpty());
    }
}
