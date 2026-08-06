package org.aventyrs.core.race;

import org.aventyrs.core.action.ActionProfile;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterEgos;
import org.aventyrs.core.character.SizeCategory;
import org.aventyrs.core.feat.FeatCategory;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Player;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MeioElfoTest {

    @Test
    void generateEmptyCharacterReturnsAUsableBuilder() {
        assertNotNull(new MeioElfo(new Human()).generateEmptyCharacter(List.of()));
    }

    @Test
    void isHumanoideAndMestico() {
        MeioElfo meioElfo = new MeioElfo(new Human());
        assertEquals(CreatureType.HUMANOIDE, meioElfo.getCreatureType());
        assertTrue(meioElfo.isMestico());
    }

    @Test
    void hasFixedDexterityRacialBonusWithNoInheritedAttributeChosen() {
        MeioElfo meioElfo = new MeioElfo(new Human());
        assertEquals(Map.of(AttributeDomain.DEXTERITY, 1), meioElfo.getFixedAttributeBonuses());
    }

    @Test
    void addsInheritedAttributeBonusWhenParentGrantsIt() {
        MeioElfo meioElfo = new MeioElfo(new Anao(), AttributeDomain.VIGOR, List.of());
        assertEquals(Map.of(AttributeDomain.DEXTERITY, 1, AttributeDomain.VIGOR, 1), meioElfo.getFixedAttributeBonuses());
    }

    @Test
    void mergesInheritedAttributeBonusWhenItMatchesTheBaseDexterityBonus() {
        MeioElfo meioElfo = new MeioElfo(new Elfo(), AttributeDomain.DEXTERITY, List.of());
        assertEquals(Map.of(AttributeDomain.DEXTERITY, 2), meioElfo.getFixedAttributeBonuses());
    }

    @Test
    void generateEmptyCharacterInheritsTheParentRacesBaseSizeCategory() {
        MeioElfo meioElfo = new MeioElfo(new Anao());
        Character character = meioElfo.generateEmptyCharacter(List.of())
                .player(new Player())
                .name("Test")
                .race(meioElfo)
                .attributes(CharacterAttributes.builder().build())
                .egos(CharacterEgos.builder().build())
                .actionProfile(ActionProfile.REFLEXOS_RAPIDOS)
                .build();

        assertEquals(SizeCategory.MINUS_ONE, character.getSizeCategory());
    }

    @Test
    void getRacialAbilitiesReturnsTheInheritedAbilitiesFromTheParentRace() {
        MeioElfo meioElfo = new MeioElfo(new Anao(), null, List.of(AnoesRacialAbility.ABATEDORES_DE_GIGANTES));
        assertEquals(List.of(AnoesRacialAbility.ABATEDORES_DE_GIGANTES), meioElfo.getRacialAbilities());
    }

    @Test
    void hasNoInheritedRacialAbilitiesByDefault() {
        assertTrue(new MeioElfo(new Human()).getRacialAbilities().isEmpty());
    }

    @Test
    void usesTheBaseCostsForFeatsAndSkills() {
        MeioElfo meioElfo = new MeioElfo(new Human());
        assertEquals(Race.BASE_NEW_FEAT_COST, meioElfo.getNewFeatCost(FeatCategory.ELFICO));
        assertEquals(Race.BASE_NEW_SKILL_COST, meioElfo.getNewSkillCost());
    }

    @Test
    void constructorRejectsANullParentRace() {
        assertThrows(NullPointerException.class, () -> new MeioElfo(null));
    }

    @Test
    void constructorRejectsAMesticoParentRace() {
        MeioElfo alreadyMestico = new MeioElfo(new Human());
        assertThrows(IllegalOperationException.class, () -> new MeioElfo(alreadyMestico));
    }

    @Test
    void constructorRejectsANonHumanoideParentRace() {
        assertThrows(IllegalOperationException.class, () -> new MeioElfo(new Fada()));
    }

    @Test
    void constructorRejectsAnInheritedAttributeTheParentRaceDoesNotGrant() {
        assertThrows(IllegalOperationException.class,
                () -> new MeioElfo(new Human(), AttributeDomain.VIGOR, List.of()));
    }

    @Test
    void constructorRejectsAnInheritedRacialAbilityNotGrantedByTheParentRace() {
        assertThrows(IllegalOperationException.class,
                () -> new MeioElfo(new Anao(), null, List.of(ElfosRacialAbility.SENTIDOS_ABSOLUTOS)));
    }

    @Test
    void constructorRejectsMoreThanTwoInheritedRacialAbilities() {
        assertThrows(IllegalOperationException.class,
                () -> new MeioElfo(new Anao(), null, List.of(
                        AnoesRacialAbility.ABATEDORES_DE_GIGANTES,
                        AnoesRacialAbility.ABATEDORES_DE_GIGANTES,
                        AnoesRacialAbility.ABATEDORES_DE_GIGANTES)));
    }
}
