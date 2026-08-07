package org.aventyrs.core.race;

import org.aventyrs.core.ability.StrengthAbility;
import org.aventyrs.core.ability.VigorAbility;
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

class NascidoDaFlorestaTest {

    @Test
    void generateEmptyCharacterReturnsAUsableBuilder() {
        assertNotNull(new NascidoDaFloresta(new Human()).generateEmptyCharacter(List.of()));
    }

    @Test
    void isHumanoideAndMestico() {
        NascidoDaFloresta nascidoDaFloresta = new NascidoDaFloresta(new Human());
        assertEquals(CreatureType.HUMANOIDE, nascidoDaFloresta.getCreatureType());
        assertTrue(nascidoDaFloresta.isMestico());
    }

    @Test
    void hasFixedCharismaAndFocusRacialBonusesRegardlessOfTheParentRace() {
        NascidoDaFloresta nascidoDaFloresta = new NascidoDaFloresta(new Anao());
        assertEquals(Map.of(AttributeDomain.CHARISMA, 1, AttributeDomain.FOCUS, 1),
                nascidoDaFloresta.getFixedAttributeBonuses());
    }

    @Test
    void generateEmptyCharacterInheritsTheParentRacesBaseSizeCategoryWithNoOffset() {
        NascidoDaFloresta nascidoDaFloresta = new NascidoDaFloresta(new Anao());
        Character character = nascidoDaFloresta.generateEmptyCharacter(List.of())
                .player(new Player())
                .name("Test")
                .race(nascidoDaFloresta)
                .attributes(CharacterAttributes.builder().build())
                .egos(CharacterEgos.builder().build())
                .actionProfile(ActionProfile.REFLEXOS_RAPIDOS)
                .build();

        assertEquals(SizeCategory.MINUS_ONE, character.getSizeCategory());
    }

    @Test
    void hasNoInheritedRacialOrAttributeAbilitiesByDefault() {
        NascidoDaFloresta nascidoDaFloresta = new NascidoDaFloresta(new Human());
        assertTrue(nascidoDaFloresta.getRacialAbilities().isEmpty());
        assertTrue(nascidoDaFloresta.getInheritedAttributeAbilities().isEmpty());
    }

    @Test
    void getRacialAbilitiesReturnsTheInheritedAbilitiesFromTheParentRace() {
        NascidoDaFloresta nascidoDaFloresta = new NascidoDaFloresta(new Anao(),
                List.of(AnoesRacialAbility.ABATEDORES_DE_GIGANTES), List.of());
        assertEquals(List.of(AnoesRacialAbility.ABATEDORES_DE_GIGANTES), nascidoDaFloresta.getRacialAbilities());
    }

    @Test
    void getInheritedAttributeAbilitiesReturnsTheChosenAbilitiesForEachAttributeTheParentGrants() {
        NascidoDaFloresta nascidoDaFloresta = new NascidoDaFloresta(new Anao(), List.of(), List.of(VigorAbility.SOBRE_HUMANO));
        assertEquals(List.of(VigorAbility.SOBRE_HUMANO), nascidoDaFloresta.getInheritedAttributeAbilities());
    }

    @Test
    void usesTheBaseCostsForFeatsAndSkills() {
        NascidoDaFloresta nascidoDaFloresta = new NascidoDaFloresta(new Human());
        assertEquals(Race.BASE_NEW_FEAT_COST, nascidoDaFloresta.getNewFeatCost(FeatCategory.METAMAGICO));
        assertEquals(Race.BASE_NEW_SKILL_COST, nascidoDaFloresta.getNewSkillCost());
    }

    @Test
    void constructorRejectsANullParentRace() {
        assertThrows(NullPointerException.class, () -> new NascidoDaFloresta(null));
    }

    @Test
    void constructorRejectsAMesticoParentRace() {
        NascidoDaFloresta alreadyMestico = new NascidoDaFloresta(new Human());
        assertThrows(IllegalOperationException.class, () -> new NascidoDaFloresta(alreadyMestico));
    }

    @Test
    void constructorRejectsANonHumanoideParentRace() {
        assertThrows(IllegalOperationException.class, () -> new NascidoDaFloresta(new Fada()));
    }

    @Test
    void constructorRejectsAnInheritedRacialAbilityNotGrantedByTheParentRace() {
        assertThrows(IllegalOperationException.class,
                () -> new NascidoDaFloresta(new Anao(), List.of(ElfosRacialAbility.SENTIDOS_ABSOLUTOS), List.of()));
    }

    @Test
    void constructorRejectsMoreThanTwoInheritedRacialAbilities() {
        assertThrows(IllegalOperationException.class,
                () -> new NascidoDaFloresta(new Anao(), List.of(
                        AnoesRacialAbility.ABATEDORES_DE_GIGANTES,
                        AnoesRacialAbility.ABATEDORES_DE_GIGANTES,
                        AnoesRacialAbility.ABATEDORES_DE_GIGANTES), List.of()));
    }

    @Test
    void constructorRejectsAnInheritedAttributeAbilityForAnAttributeTheParentDoesNotGrant() {
        assertThrows(IllegalOperationException.class,
                () -> new NascidoDaFloresta(new Human(), List.of(), List.of(VigorAbility.SOBRE_HUMANO)));
    }

    @Test
    void constructorRejectsTwoInheritedAttributeAbilitiesForTheSameAttribute() {
        assertThrows(IllegalOperationException.class,
                () -> new NascidoDaFloresta(new Anao(), List.of(),
                        List.of(VigorAbility.SOBRE_HUMANO, VigorAbility.METABOLISMO_RAPIDO)));
    }

    @Test
    void constructorAcceptsOneInheritedAttributeAbilityPerAttributeTheParentGrants() {
        NascidoDaFloresta nascidoDaFloresta = new NascidoDaFloresta(new Anao(), List.of(), List.of(VigorAbility.SOBRE_HUMANO));
        assertEquals(List.of(VigorAbility.SOBRE_HUMANO), nascidoDaFloresta.getInheritedAttributeAbilities());
    }

    @Test
    void strengthAbilityIsUnrelatedToWhatAnaoGrantsAndIsThereforeRejected() {
        assertThrows(IllegalOperationException.class,
                () -> new NascidoDaFloresta(new Anao(), List.of(), List.of(StrengthAbility.SUBJUGAR)));
    }
}
