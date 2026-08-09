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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AquanTest {

    @Test
    void isMestico() {
        assertTrue(new Aquan(new Human()).isMestico());
    }

    @Test
    void delegatesCreatureTypeToTheParentRace() {
        assertEquals(CreatureType.HUMANOIDE, new Aquan(new Human()).getCreatureType());
        assertEquals(CreatureType.FEERICO, new Aquan(new Fada()).getCreatureType());
    }

    @Test
    void hasFixedAttributeBonusesWhenParentDoesNotGrantDexterity() {
        Aquan aquan = new Aquan(new Human());
        assertEquals(Map.of(AttributeDomain.DEXTERITY, 2, AttributeDomain.VIGOR, -1), aquan.getFixedAttributeBonuses());
    }

    @Test
    void primaryBonusIncreasesToThreeWhenParentAlsoGrantsDexterity() {
        Aquan aquan = new Aquan(new Elfo());
        assertEquals(Map.of(AttributeDomain.DEXTERITY, 3, AttributeDomain.VIGOR, -1), aquan.getFixedAttributeBonuses());
    }

    @Test
    void generateEmptyCharacterInheritsTheParentRacesBaseSizeCategoryWithNoOffset() {
        Aquan aquan = new Aquan(new Anao());
        Character character = aquan.generateEmptyCharacter(List.of())
                .player(new Player())
                .name("Test")
                .race(aquan)
                .attributes(CharacterAttributes.builder().build())
                .egos(CharacterEgos.builder().build())
                .actionProfile(ActionProfile.REFLEXOS_RAPIDOS)
                .build();

        assertEquals(SizeCategory.MINUS_ONE, character.getSizeCategory());
    }

    @Test
    void hasNoInheritedRacialOrAttributeAbilitiesByDefault() {
        Aquan aquan = new Aquan(new Human());
        assertTrue(aquan.getRacialAbilities().isEmpty());
        assertTrue(aquan.getInheritedAttributeAbilities().isEmpty());
    }

    @Test
    void getRacialAbilitiesReturnsTheInheritedAbilitiesFromTheParentRace() {
        Aquan aquan = new Aquan(new Anao(), List.of(AnoesRacialAbility.ABATEDORES_DE_GIGANTES), List.of());
        assertEquals(List.of(AnoesRacialAbility.ABATEDORES_DE_GIGANTES), aquan.getRacialAbilities());
    }

    @Test
    void getInheritedAttributeAbilitiesReturnsTheChosenAbilitiesForEachAttributeTheParentGrants() {
        Aquan aquan = new Aquan(new Anao(), List.of(), List.of(VigorAbility.SOBRE_HUMANO));
        assertEquals(List.of(VigorAbility.SOBRE_HUMANO), aquan.getInheritedAttributeAbilities());
    }

    @Test
    void usesTheBaseCostsForFeatsAndSkills() {
        Aquan aquan = new Aquan(new Human());
        assertEquals(Race.BASE_NEW_FEAT_COST, aquan.getNewFeatCost(FeatCategory.MOBILIDADE));
        assertEquals(Race.BASE_NEW_SKILL_COST, aquan.getNewSkillCost());
    }

    @Test
    void constructorRejectsANullParentRace() {
        assertThrows(NullPointerException.class, () -> new Aquan(null));
    }

    @Test
    void constructorRejectsAMesticoParentRace() {
        Aquan alreadyMestico = new Aquan(new Human());
        assertThrows(IllegalOperationException.class, () -> new Aquan(alreadyMestico));
    }

    @Test
    void constructorRejectsAnInheritedRacialAbilityNotGrantedByTheParentRace() {
        assertThrows(IllegalOperationException.class,
                () -> new Aquan(new Anao(), List.of(ElfosRacialAbility.SENTIDOS_ABSOLUTOS), List.of()));
    }

    @Test
    void constructorRejectsMoreThanTwoInheritedRacialAbilities() {
        assertThrows(IllegalOperationException.class,
                () -> new Aquan(new Anao(), List.of(
                        AnoesRacialAbility.ABATEDORES_DE_GIGANTES,
                        AnoesRacialAbility.ABATEDORES_DE_GIGANTES,
                        AnoesRacialAbility.ABATEDORES_DE_GIGANTES), List.of()));
    }

    @Test
    void constructorRejectsAnInheritedAttributeAbilityForAnAttributeTheParentDoesNotGrant() {
        assertThrows(IllegalOperationException.class,
                () -> new Aquan(new Human(), List.of(), List.of(VigorAbility.SOBRE_HUMANO)));
    }

    @Test
    void constructorRejectsTwoInheritedAttributeAbilitiesForTheSameAttribute() {
        assertThrows(IllegalOperationException.class,
                () -> new Aquan(new Anao(), List.of(), List.of(VigorAbility.SOBRE_HUMANO, VigorAbility.METABOLISMO_RAPIDO)));
    }

    @Test
    void constructorAcceptsOneInheritedAttributeAbilityPerAttributeTheParentGrants() {
        Aquan aquan = new Aquan(new Anao(), List.of(), List.of(VigorAbility.SOBRE_HUMANO));
        assertEquals(List.of(VigorAbility.SOBRE_HUMANO), aquan.getInheritedAttributeAbilities());
    }

    @Test
    void strengthAbilityIsUnrelatedToWhatAnaoGrantsAndIsThereforeRejected() {
        assertThrows(IllegalOperationException.class,
                () -> new Aquan(new Anao(), List.of(), List.of(StrengthAbility.SUBJUGAR)));
    }
}
