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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HomemFeraTest {

    private final HomemFera licantropo = new HomemFera(HomemFera.EspiritoAnimal.LICANTROPO);

    @Test
    void generateEmptyCharacterLeavesTheDefaultZeroSizeCategory() {
        Character character = licantropo.generateEmptyCharacter(List.of())
                .player(new Player())
                .name("Test")
                .race(licantropo)
                .attributes(CharacterAttributes.builder().build())
                .egos(CharacterEgos.builder().build())
                .actionProfile(ActionProfile.REFLEXOS_RAPIDOS)
                .build();

        assertEquals(SizeCategory.ZERO, character.getSizeCategory());
    }

    @Test
    void hasHumanoideCreatureTypeAndZeroBaseSizeCategory() {
        assertEquals(CreatureType.HUMANOIDE, licantropo.getCreatureType());
        assertEquals(SizeCategory.ZERO, licantropo.getBaseSizeCategory());
    }

    @Test
    void hasAFixedInstinctRacialBonusWhicheverSpiritManifested() {
        for (HomemFera.EspiritoAnimal espirito : HomemFera.EspiritoAnimal.values()) {
            assertEquals(Map.of(AttributeDomain.INSTINCT, 1),
                    new HomemFera(espirito).getFixedAttributeBonuses());
        }
    }

    @Test
    void everySpiritRecordsATwoAttributePair() {
        for (HomemFera.EspiritoAnimal espirito : HomemFera.EspiritoAnimal.values()) {
            assertEquals(2, espirito.getAttributePair().size());
        }
        assertEquals(Set.of(AttributeDomain.CHARISMA, AttributeDomain.STRENGTH),
                HomemFera.EspiritoAnimal.LICANTROPO.getAttributePair());
        assertEquals(Set.of(AttributeDomain.STRENGTH, AttributeDomain.FOCUS),
                HomemFera.EspiritoAnimal.TORNAT.getAttributePair());
    }

    @Test
    void theSourceDocumentListsSevenSpiritsDespiteSayingSix() {
        assertEquals(7, HomemFera.EspiritoAnimal.values().length);
    }

    @Test
    void hasNoChoosableRacialBonuses() {
        assertEquals(0, licantropo.getChoosableAttributeBonusPoints());
        assertTrue(licantropo.getChoosableAttributes().isEmpty());
    }

    @Test
    void usesTheBaseCostsForFeatsAndSkills() {
        assertEquals(Race.BASE_NEW_FEAT_COST, licantropo.getNewFeatCost(FeatCategory.FERAL));
        assertEquals(Race.BASE_NEW_SKILL_COST, licantropo.getNewSkillCost());
    }

    @Test
    void isNotMestico() {
        assertFalse(licantropo.isMestico());
    }

    @Test
    void grantsFortalecimentoFeralAsARacialAbility() {
        assertEquals(List.of(HomensFeraRacialAbility.FORTALECIMENTO_FERAL), licantropo.getRacialAbilities());
    }

    @Test
    void grantsNoCriticalEffectImmunities() {
        assertTrue(licantropo.getCriticalEffectImmunities().isEmpty());
    }
}
