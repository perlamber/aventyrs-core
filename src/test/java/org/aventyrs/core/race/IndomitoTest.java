package org.aventyrs.core.race;

import org.aventyrs.core.action.ActionProfile;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterEgos;
import org.aventyrs.core.character.SizeCategory;
import org.aventyrs.core.feat.FeatCategory;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IndomitoTest {

    private final Indomito apedemak = new Indomito(Indomito.Tribo.APEDEMAK);

    @Test
    void generateEmptyCharacterLeavesTheDefaultZeroSizeCategory() {
        Character character = apedemak.generateEmptyCharacter(List.of())
                .player(new Player())
                .name("Test")
                .race(apedemak)
                .attributes(CharacterAttributes.builder().build())
                .egos(CharacterEgos.builder().build())
                .actionProfile(ActionProfile.REFLEXOS_RAPIDOS)
                .build();

        assertEquals(SizeCategory.ZERO, character.getSizeCategory());
    }

    @Test
    void hasMonstruosoCreatureTypeAndZeroBaseSizeCategory() {
        assertEquals(CreatureType.MONSTRUOSO, apedemak.getCreatureType());
        assertEquals(SizeCategory.ZERO, apedemak.getBaseSizeCategory());
    }

    @Test
    void everyTriboGrantsOneStrengthPlusItsOwnBonus() {
        assertEquals(Map.of(AttributeDomain.STRENGTH, 1, AttributeDomain.INSTINCT, 1),
                new Indomito(Indomito.Tribo.APEDEMAK).getFixedAttributeBonuses());
        assertEquals(Map.of(AttributeDomain.STRENGTH, 1, AttributeDomain.FOCUS, 1),
                new Indomito(Indomito.Tribo.BASTET).getFixedAttributeBonuses());
        assertEquals(Map.of(AttributeDomain.STRENGTH, 1, AttributeDomain.GNOSE, 1),
                new Indomito(Indomito.Tribo.SACMIS).getFixedAttributeBonuses());
    }

    @Test
    void anImpuroMergesItsSecondStrengthPointIntoASingleEntry() {
        assertEquals(Map.of(AttributeDomain.STRENGTH, 2, AttributeDomain.VIGOR, 1),
                new Indomito(Indomito.Tribo.IMPURO).getFixedAttributeBonuses());
    }

    @Test
    void everyTriboRecordsItsAdditionalTraining() {
        assertEquals(List.of(SkillType.ESQUIVA_E_APARAR), Indomito.Tribo.APEDEMAK.getAdditionalTraining());
        assertEquals(List.of(SkillType.FURTIVIDADE), Indomito.Tribo.BASTET.getAdditionalTraining());
        assertEquals(List.of(SkillType.CONHECIMENTOS, SkillType.MEDICINA_E_CURA),
                Indomito.Tribo.SACMIS.getAdditionalTraining());
        assertEquals(List.of(SkillType.ATLETISMO), Indomito.Tribo.IMPURO.getAdditionalTraining());
    }

    @Test
    void hasNoChoosableRacialBonuses() {
        assertEquals(0, apedemak.getChoosableAttributeBonusPoints());
        assertTrue(apedemak.getChoosableAttributes().isEmpty());
    }

    @Test
    void usesTheBaseCostsForFeatsAndSkills() {
        assertEquals(Race.BASE_NEW_FEAT_COST, apedemak.getNewFeatCost(FeatCategory.INDOMITO));
        assertEquals(Race.BASE_NEW_SKILL_COST, apedemak.getNewSkillCost());
    }

    @Test
    void isNotMesticoAndGrantsNoRacialAbilities() {
        assertFalse(apedemak.isMestico());
        assertTrue(apedemak.getRacialAbilities().isEmpty());
        assertTrue(apedemak.getCriticalEffectImmunities().isEmpty());
    }
}
