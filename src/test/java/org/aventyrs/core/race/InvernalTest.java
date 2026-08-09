package org.aventyrs.core.race;

import org.aventyrs.core.ability.CharismaAbility;
import org.aventyrs.core.ability.StrengthAbility;
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

class InvernalTest {

    @Test
    void isMestico() {
        assertTrue(new Invernal(new Human()).isMestico());
    }

    @Test
    void delegatesCreatureTypeToTheParentRace() {
        assertEquals(CreatureType.HUMANOIDE, new Invernal(new Human()).getCreatureType());
        assertEquals(CreatureType.FEERICO, new Invernal(new Fada()).getCreatureType());
    }

    @Test
    void hasFixedAttributeBonusesWhenParentDoesNotGrantStrength() {
        Invernal invernal = new Invernal(new Human());
        assertEquals(Map.of(AttributeDomain.STRENGTH, 2, AttributeDomain.CHARISMA, -1), invernal.getFixedAttributeBonuses());
    }

    @Test
    void primaryBonusIncreasesToThreeWhenParentAlsoGrantsStrength() {
        Invernal invernal = new Invernal(new Gigantes());
        assertEquals(Map.of(AttributeDomain.STRENGTH, 3, AttributeDomain.CHARISMA, -1), invernal.getFixedAttributeBonuses());
    }

    @Test
    void generateEmptyCharacterInheritsTheParentRacesBaseSizeCategoryShiftedByOne() {
        Invernal invernal = new Invernal(new Anao());
        Character character = invernal.generateEmptyCharacter(List.of())
                .player(new Player())
                .name("Test")
                .race(invernal)
                .attributes(CharacterAttributes.builder().build())
                .egos(CharacterEgos.builder().build())
                .actionProfile(ActionProfile.REFLEXOS_RAPIDOS)
                .build();

        assertEquals(SizeCategory.ZERO, character.getSizeCategory());
    }

    @Test
    void hasNoInheritedRacialOrAttributeAbilitiesByDefault() {
        Invernal invernal = new Invernal(new Human());
        assertTrue(invernal.getRacialAbilities().isEmpty());
        assertTrue(invernal.getInheritedAttributeAbilities().isEmpty());
    }

    @Test
    void getRacialAbilitiesReturnsTheInheritedAbilitiesFromTheParentRace() {
        Invernal invernal = new Invernal(new Anao(), List.of(AnoesRacialAbility.ABATEDORES_DE_GIGANTES), List.of());
        assertEquals(List.of(AnoesRacialAbility.ABATEDORES_DE_GIGANTES), invernal.getRacialAbilities());
    }

    @Test
    void usesTheBaseCostsForFeatsAndSkills() {
        Invernal invernal = new Invernal(new Human());
        assertEquals(Race.BASE_NEW_FEAT_COST, invernal.getNewFeatCost(FeatCategory.DUELISTA));
        assertEquals(Race.BASE_NEW_SKILL_COST, invernal.getNewSkillCost());
    }

    @Test
    void constructorRejectsANullParentRace() {
        assertThrows(NullPointerException.class, () -> new Invernal(null));
    }

    @Test
    void constructorRejectsAMesticoParentRace() {
        Invernal alreadyMestico = new Invernal(new Human());
        assertThrows(IllegalOperationException.class, () -> new Invernal(alreadyMestico));
    }

    @Test
    void constructorRejectsAnInheritedRacialAbilityNotGrantedByTheParentRace() {
        assertThrows(IllegalOperationException.class,
                () -> new Invernal(new Anao(), List.of(ElfosRacialAbility.SENTIDOS_ABSOLUTOS), List.of()));
    }

    @Test
    void constructorRejectsAnInheritedAttributeAbilityForAnAttributeTheParentDoesNotGrant() {
        assertThrows(IllegalOperationException.class,
                () -> new Invernal(new Human(), List.of(), List.of(StrengthAbility.SUBJUGAR)));
    }

    @Test
    void constructorAcceptsOneInheritedAttributeAbilityPerAttributeTheParentGrants() {
        Invernal invernal = new Invernal(new Gigantes(), List.of(), List.of(StrengthAbility.SUBJUGAR));
        assertEquals(List.of(StrengthAbility.SUBJUGAR), invernal.getInheritedAttributeAbilities());
    }

    @Test
    void charismaAbilityIsUnrelatedToWhatGigantesGrantsAndIsThereforeRejected() {
        assertThrows(IllegalOperationException.class,
                () -> new Invernal(new Gigantes(), List.of(), List.of(CharismaAbility.AGRESSAO_ANUNCIADA)));
    }
}
