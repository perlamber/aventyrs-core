package org.aventyrs.core.race;

import org.aventyrs.core.ability.FocusAbility;
import org.aventyrs.core.ability.InstinctAbility;
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

class FlaminideoTest {

    @Test
    void isMestico() {
        assertTrue(new Flaminideo(new Human()).isMestico());
    }

    @Test
    void delegatesCreatureTypeToTheParentRace() {
        assertEquals(CreatureType.HUMANOIDE, new Flaminideo(new Human()).getCreatureType());
        assertEquals(CreatureType.FEERICO, new Flaminideo(new Fada()).getCreatureType());
    }

    @Test
    void hasFixedAttributeBonusesWhenParentDoesNotGrantInstinct() {
        Flaminideo flaminideo = new Flaminideo(new Human());
        assertEquals(Map.of(AttributeDomain.INSTINCT, 2, AttributeDomain.FOCUS, -1), flaminideo.getFixedAttributeBonuses());
    }

    @Test
    void primaryBonusIncreasesToThreeWhenParentAlsoGrantsInstinct() {
        Flaminideo flaminideo = new Flaminideo(new Satiro());
        assertEquals(Map.of(AttributeDomain.INSTINCT, 3, AttributeDomain.FOCUS, -1), flaminideo.getFixedAttributeBonuses());
    }

    @Test
    void generateEmptyCharacterInheritsTheParentRacesBaseSizeCategoryWithNoOffset() {
        Flaminideo flaminideo = new Flaminideo(new Gigantes());
        Character character = flaminideo.generateEmptyCharacter(List.of())
                .player(new Player())
                .name("Test")
                .race(flaminideo)
                .attributes(CharacterAttributes.builder().build())
                .egos(CharacterEgos.builder().build())
                .actionProfile(ActionProfile.REFLEXOS_RAPIDOS)
                .build();

        assertEquals(SizeCategory.PLUS_TWO, character.getSizeCategory());
    }

    @Test
    void hasNoInheritedRacialOrAttributeAbilitiesByDefault() {
        Flaminideo flaminideo = new Flaminideo(new Human());
        assertTrue(flaminideo.getRacialAbilities().isEmpty());
        assertTrue(flaminideo.getInheritedAttributeAbilities().isEmpty());
    }

    @Test
    void getRacialAbilitiesReturnsTheInheritedAbilitiesFromTheParentRace() {
        Flaminideo flaminideo = new Flaminideo(new Anao(), List.of(AnoesRacialAbility.ABATEDORES_DE_GIGANTES), List.of());
        assertEquals(List.of(AnoesRacialAbility.ABATEDORES_DE_GIGANTES), flaminideo.getRacialAbilities());
    }

    @Test
    void usesTheBaseCostsForFeatsAndSkills() {
        Flaminideo flaminideo = new Flaminideo(new Human());
        assertEquals(Race.BASE_NEW_FEAT_COST, flaminideo.getNewFeatCost(FeatCategory.ESCUDEIRO));
        assertEquals(Race.BASE_NEW_SKILL_COST, flaminideo.getNewSkillCost());
    }

    @Test
    void constructorRejectsANullParentRace() {
        assertThrows(NullPointerException.class, () -> new Flaminideo(null));
    }

    @Test
    void constructorRejectsAMesticoParentRace() {
        Flaminideo alreadyMestico = new Flaminideo(new Human());
        assertThrows(IllegalOperationException.class, () -> new Flaminideo(alreadyMestico));
    }

    @Test
    void constructorRejectsAnInheritedRacialAbilityNotGrantedByTheParentRace() {
        assertThrows(IllegalOperationException.class,
                () -> new Flaminideo(new Anao(), List.of(ElfosRacialAbility.SENTIDOS_ABSOLUTOS), List.of()));
    }

    @Test
    void constructorRejectsAnInheritedAttributeAbilityForAnAttributeTheParentDoesNotGrant() {
        assertThrows(IllegalOperationException.class,
                () -> new Flaminideo(new Human(), List.of(), List.of(InstinctAbility.SENTIR_A_INTENCAO)));
    }

    @Test
    void constructorAcceptsOneInheritedAttributeAbilityPerAttributeTheParentGrants() {
        Flaminideo flaminideo = new Flaminideo(new Satiro(), List.of(), List.of(InstinctAbility.SENTIR_A_INTENCAO));
        assertEquals(List.of(InstinctAbility.SENTIR_A_INTENCAO), flaminideo.getInheritedAttributeAbilities());
    }

    @Test
    void focusAbilityIsUnrelatedToWhatSatiroGrantsAndIsThereforeRejected() {
        assertThrows(IllegalOperationException.class,
                () -> new Flaminideo(new Satiro(), List.of(), List.of(FocusAbility.CONCENTRACAO_PROFUNDA)));
    }
}
