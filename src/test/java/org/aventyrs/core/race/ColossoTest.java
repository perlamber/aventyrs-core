package org.aventyrs.core.race;

import org.aventyrs.core.ability.DexterityAbility;
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

class ColossoTest {

    @Test
    void isMestico() {
        assertTrue(new Colosso(new Human()).isMestico());
    }

    @Test
    void delegatesCreatureTypeToTheParentRace() {
        assertEquals(CreatureType.HUMANOIDE, new Colosso(new Human()).getCreatureType());
        assertEquals(CreatureType.FEERICO, new Colosso(new Fada()).getCreatureType());
    }

    @Test
    void hasFixedAttributeBonusesWhenParentDoesNotGrantVigor() {
        Colosso colosso = new Colosso(new Human());
        assertEquals(Map.of(AttributeDomain.VIGOR, 2, AttributeDomain.DEXTERITY, -1), colosso.getFixedAttributeBonuses());
    }

    @Test
    void primaryBonusIncreasesToThreeWhenParentAlsoGrantsVigor() {
        Colosso colosso = new Colosso(new Anao());
        assertEquals(Map.of(AttributeDomain.VIGOR, 3, AttributeDomain.DEXTERITY, -1), colosso.getFixedAttributeBonuses());
    }

    @Test
    void generateEmptyCharacterInheritsTheParentRacesBaseSizeCategoryShiftedByOne() {
        Colosso colosso = new Colosso(new Anao());
        Character character = colosso.generateEmptyCharacter(List.of())
                .player(new Player())
                .name("Test")
                .race(colosso)
                .attributes(CharacterAttributes.builder().build())
                .egos(CharacterEgos.builder().build())
                .actionProfile(ActionProfile.REFLEXOS_RAPIDOS)
                .build();

        assertEquals(SizeCategory.ZERO, character.getSizeCategory());
    }

    @Test
    void hasNoInheritedRacialOrAttributeAbilitiesByDefault() {
        Colosso colosso = new Colosso(new Human());
        assertTrue(colosso.getRacialAbilities().isEmpty());
        assertTrue(colosso.getInheritedAttributeAbilities().isEmpty());
    }

    @Test
    void getRacialAbilitiesReturnsTheInheritedAbilitiesFromTheParentRace() {
        Colosso colosso = new Colosso(new Anao(), List.of(AnoesRacialAbility.ABATEDORES_DE_GIGANTES), List.of());
        assertEquals(List.of(AnoesRacialAbility.ABATEDORES_DE_GIGANTES), colosso.getRacialAbilities());
    }

    @Test
    void usesTheBaseCostsForFeatsAndSkills() {
        Colosso colosso = new Colosso(new Human());
        assertEquals(Race.BASE_NEW_FEAT_COST, colosso.getNewFeatCost(FeatCategory.SOBREVIVENCIA));
        assertEquals(Race.BASE_NEW_SKILL_COST, colosso.getNewSkillCost());
    }

    @Test
    void constructorRejectsANullParentRace() {
        assertThrows(NullPointerException.class, () -> new Colosso(null));
    }

    @Test
    void constructorRejectsAMesticoParentRace() {
        Colosso alreadyMestico = new Colosso(new Human());
        assertThrows(IllegalOperationException.class, () -> new Colosso(alreadyMestico));
    }

    @Test
    void constructorRejectsAnInheritedRacialAbilityNotGrantedByTheParentRace() {
        assertThrows(IllegalOperationException.class,
                () -> new Colosso(new Anao(), List.of(ElfosRacialAbility.SENTIDOS_ABSOLUTOS), List.of()));
    }

    @Test
    void constructorRejectsAnInheritedAttributeAbilityForAnAttributeTheParentDoesNotGrant() {
        assertThrows(IllegalOperationException.class,
                () -> new Colosso(new Human(), List.of(), List.of(VigorAbility.SOBRE_HUMANO)));
    }

    @Test
    void constructorAcceptsOneInheritedAttributeAbilityPerAttributeTheParentGrants() {
        Colosso colosso = new Colosso(new Anao(), List.of(), List.of(VigorAbility.SOBRE_HUMANO));
        assertEquals(List.of(VigorAbility.SOBRE_HUMANO), colosso.getInheritedAttributeAbilities());
    }

    @Test
    void dexterityAbilityIsUnrelatedToWhatAnaoGrantsAndIsThereforeRejected() {
        assertThrows(IllegalOperationException.class,
                () -> new Colosso(new Anao(), List.of(), List.of(DexterityAbility.PASSOS_LONGOS)));
    }
}
