package org.aventyrs.core.race;

import org.aventyrs.core.ability.CharismaAbility;
import org.aventyrs.core.ability.GnoseAbility;
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

class DolosTest {

    @Test
    void isMestico() {
        assertTrue(new Dolos(new Human()).isMestico());
    }

    @Test
    void delegatesCreatureTypeToTheParentRace() {
        assertEquals(CreatureType.HUMANOIDE, new Dolos(new Human()).getCreatureType());
        assertEquals(CreatureType.FEERICO, new Dolos(new Fada()).getCreatureType());
    }

    @Test
    void hasFixedAttributeBonusesWhenParentDoesNotGrantCharisma() {
        Dolos dolos = new Dolos(new Human());
        assertEquals(Map.of(AttributeDomain.CHARISMA, 2, AttributeDomain.VIGOR, -1), dolos.getFixedAttributeBonuses());
    }

    @Test
    void primaryBonusIncreasesToThreeWhenParentAlsoGrantsCharisma() {
        Dolos dolos = new Dolos(new Gnomo());
        assertEquals(Map.of(AttributeDomain.CHARISMA, 3, AttributeDomain.VIGOR, -1), dolos.getFixedAttributeBonuses());
    }

    @Test
    void generateEmptyCharacterInheritsTheParentRacesBaseSizeCategoryShiftedByMinusOne() {
        Dolos dolos = new Dolos(new Gigantes());
        Character character = dolos.generateEmptyCharacter(List.of())
                .player(new Player())
                .name("Test")
                .race(dolos)
                .attributes(CharacterAttributes.builder().build())
                .egos(CharacterEgos.builder().build())
                .actionProfile(ActionProfile.REFLEXOS_RAPIDOS)
                .build();

        assertEquals(SizeCategory.PLUS_ONE, character.getSizeCategory());
    }

    @Test
    void hasNoInheritedRacialOrAttributeAbilitiesByDefault() {
        Dolos dolos = new Dolos(new Human());
        assertTrue(dolos.getRacialAbilities().isEmpty());
        assertTrue(dolos.getInheritedAttributeAbilities().isEmpty());
    }

    @Test
    void getRacialAbilitiesReturnsTheInheritedAbilitiesFromTheParentRace() {
        Dolos dolos = new Dolos(new Anao(), List.of(AnoesRacialAbility.ABATEDORES_DE_GIGANTES), List.of());
        assertEquals(List.of(AnoesRacialAbility.ABATEDORES_DE_GIGANTES), dolos.getRacialAbilities());
    }

    @Test
    void usesTheBaseCostsForFeatsAndSkills() {
        Dolos dolos = new Dolos(new Human());
        assertEquals(Race.BASE_NEW_FEAT_COST, dolos.getNewFeatCost(FeatCategory.ASSASSINO));
        assertEquals(Race.BASE_NEW_SKILL_COST, dolos.getNewSkillCost());
    }

    @Test
    void constructorRejectsANullParentRace() {
        assertThrows(NullPointerException.class, () -> new Dolos(null));
    }

    @Test
    void constructorRejectsAMesticoParentRace() {
        Dolos alreadyMestico = new Dolos(new Human());
        assertThrows(IllegalOperationException.class, () -> new Dolos(alreadyMestico));
    }

    @Test
    void constructorRejectsAnInheritedRacialAbilityNotGrantedByTheParentRace() {
        assertThrows(IllegalOperationException.class,
                () -> new Dolos(new Anao(), List.of(ElfosRacialAbility.SENTIDOS_ABSOLUTOS), List.of()));
    }

    @Test
    void constructorRejectsAnInheritedAttributeAbilityForAnAttributeTheParentDoesNotGrant() {
        assertThrows(IllegalOperationException.class,
                () -> new Dolos(new Human(), List.of(), List.of(GnoseAbility.DOMINIO_DO_CONHECIMENTO)));
    }

    @Test
    void constructorAcceptsOneInheritedAttributeAbilityPerAttributeTheParentGrants() {
        Dolos dolos = new Dolos(new Anao(), List.of(), List.of(GnoseAbility.DOMINIO_DO_CONHECIMENTO));
        assertEquals(List.of(GnoseAbility.DOMINIO_DO_CONHECIMENTO), dolos.getInheritedAttributeAbilities());
    }

    @Test
    void charismaAbilityIsUnrelatedToWhatAnaoGrantsAndIsThereforeRejected() {
        assertThrows(IllegalOperationException.class,
                () -> new Dolos(new Anao(), List.of(), List.of(CharismaAbility.AGRESSAO_ANUNCIADA)));
    }
}
