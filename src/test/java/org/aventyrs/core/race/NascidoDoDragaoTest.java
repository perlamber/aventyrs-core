package org.aventyrs.core.race;

import org.aventyrs.core.action.ActionProfile;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterEgos;
import org.aventyrs.core.character.SizeCategory;
import org.aventyrs.core.character.services.HitPointsService;
import org.aventyrs.core.feat.FeatCategory;
import org.aventyrs.core.magic.ElementalType;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Player;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NascidoDoDragaoTest {

    private Character buildCharacter(final NascidoDoDragao race) {
        return race.generateEmptyCharacter(List.of())
                .player(new Player())
                .name("Test")
                .race(race)
                .attributes(CharacterAttributes.builder().build())
                .egos(CharacterEgos.builder().build())
                .actionProfile(ActionProfile.REFLEXOS_RAPIDOS)
                .build();
    }

    @Test
    void generateEmptyCharacterReturnsAUsableBuilder() {
        assertNotNull(new NascidoDoDragao(new Human(), ElementalType.FOGO).generateEmptyCharacter(List.of()));
    }

    @Test
    void isHumanoideAndMestico() {
        NascidoDoDragao race = new NascidoDoDragao(new Human(), ElementalType.FOGO);
        assertEquals(CreatureType.HUMANOIDE, race.getCreatureType());
        assertTrue(race.isMestico());
    }

    @Test
    void hasFixedStrengthAndFocusRacialBonusesWithNoInheritedAttributeChosen() {
        NascidoDoDragao race = new NascidoDoDragao(new Human(), ElementalType.GELO);
        assertEquals(Map.of(AttributeDomain.STRENGTH, 1, AttributeDomain.FOCUS, 1), race.getFixedAttributeBonuses());
    }

    @Test
    void addsInheritedAttributeBonusWhenTheParentGrantsIt() {
        NascidoDoDragao race = new NascidoDoDragao(new Anao(), ElementalType.TERRA, AttributeDomain.VIGOR, List.of());
        assertEquals(Map.of(AttributeDomain.STRENGTH, 1, AttributeDomain.FOCUS, 1, AttributeDomain.VIGOR, 1),
                race.getFixedAttributeBonuses());
    }

    @Test
    void mergesInheritedAttributeBonusWhenItMatchesOneOfTheBaseBonuses() {
        NascidoDoDragao race = new NascidoDoDragao(new Orc(), ElementalType.AGUA, AttributeDomain.VIGOR, List.of());
        assertEquals(Map.of(AttributeDomain.STRENGTH, 1, AttributeDomain.FOCUS, 1, AttributeDomain.VIGOR, 1),
                race.getFixedAttributeBonuses());

        NascidoDoDragao gigantesChild = new NascidoDoDragao(new Gigantes(), ElementalType.MAGMA, AttributeDomain.STRENGTH, List.of());
        assertEquals(Map.of(AttributeDomain.STRENGTH, 2, AttributeDomain.FOCUS, 1),
                gigantesChild.getFixedAttributeBonuses());
    }

    @Test
    void rejectsAnInheritedAttributeTheParentDoesNotGrant() {
        assertThrows(IllegalOperationException.class,
                () -> new NascidoDoDragao(new Human(), ElementalType.FOGO, AttributeDomain.CHARISMA, List.of()));
    }

    @Test
    void isOneSizeCategoryAboveTheParentRace() {
        assertEquals(SizeCategory.PLUS_ONE, new NascidoDoDragao(new Human(), ElementalType.FOGO).getBaseSizeCategory());
        assertEquals(SizeCategory.ZERO, new NascidoDoDragao(new Anao(), ElementalType.FOGO).getBaseSizeCategory());
        assertEquals(SizeCategory.PLUS_THREE, new NascidoDoDragao(new Gigantes(), ElementalType.FOGO).getBaseSizeCategory());
    }

    @Test
    void generateEmptyCharacterSeedsTheShiftedSizeCategory() {
        assertEquals(SizeCategory.ZERO, buildCharacter(new NascidoDoDragao(new Anao(), ElementalType.FOGO)).getSizeCategory());
    }

    @Test
    void generateEmptyCharacterSeedsTheArmaduraDraconicaLifeMultiplier() {
        Character character = buildCharacter(new NascidoDoDragao(new Human(), ElementalType.FOGO));
        assertEquals(HitPointsService.DEFAULT_LIFE_MULTIPLIER + 1, character.getLifeMultiplier());
    }

    @Test
    void escamasCromaticaPairsEveryElementWithItsOpposite() {
        assertEquals(ElementalType.AGUA, new NascidoDoDragao(new Human(), ElementalType.FOGO).getOpposedElementalType());
        assertEquals(ElementalType.FOGO, new NascidoDoDragao(new Human(), ElementalType.AGUA).getOpposedElementalType());
        assertEquals(ElementalType.GELO, new NascidoDoDragao(new Human(), ElementalType.MAGMA).getOpposedElementalType());
        assertEquals(ElementalType.MAGMA, new NascidoDoDragao(new Human(), ElementalType.GELO).getOpposedElementalType());
        assertEquals(ElementalType.AR, new NascidoDoDragao(new Human(), ElementalType.TERRA).getOpposedElementalType());
        assertEquals(ElementalType.TERRA, new NascidoDoDragao(new Human(), ElementalType.AR).getOpposedElementalType());
        assertEquals(ElementalType.ELETRICIDADE, new NascidoDoDragao(new Human(), ElementalType.NATURAL).getOpposedElementalType());
        assertEquals(ElementalType.NATURAL, new NascidoDoDragao(new Human(), ElementalType.ELETRICIDADE).getOpposedElementalType());
    }

    @Test
    void rejectsTodosAsAnElementalLineage() {
        assertThrows(IllegalOperationException.class, () -> new NascidoDoDragao(new Human(), ElementalType.TODOS));
    }

    @Test
    void rejectsAMesticoParentRace() {
        assertThrows(IllegalOperationException.class,
                () -> new NascidoDoDragao(new MeioElfo(new Human()), ElementalType.FOGO));
    }

    @Test
    void rejectsANonHumanoideParentRace() {
        assertThrows(IllegalOperationException.class, () -> new NascidoDoDragao(new Fada(), ElementalType.FOGO));
    }

    @Test
    void inheritsUpToTwoOfTheParentRacesOwnRacialAbilities() {
        NascidoDoDragao race = new NascidoDoDragao(new Anao(), ElementalType.FOGO, null,
                List.of(AnoesRacialAbility.ABATEDORES_DE_GIGANTES));
        assertEquals(List.of(AnoesRacialAbility.ABATEDORES_DE_GIGANTES), race.getRacialAbilities());
    }

    @Test
    void rejectsAnInheritedAbilityTheParentRaceDoesNotHave() {
        assertThrows(IllegalOperationException.class,
                () -> new NascidoDoDragao(new Human(), ElementalType.FOGO, null,
                        List.of(AnoesRacialAbility.ABATEDORES_DE_GIGANTES)));
    }

    @Test
    void usesTheBaseCostsForFeatsAndSkills() {
        NascidoDoDragao race = new NascidoDoDragao(new Human(), ElementalType.FOGO);
        assertEquals(Race.BASE_NEW_FEAT_COST, race.getNewFeatCost(FeatCategory.DRACONICO));
        assertEquals(Race.BASE_NEW_SKILL_COST, race.getNewSkillCost());
    }

    @Test
    void grantsNoCriticalEffectImmunities() {
        assertTrue(new NascidoDoDragao(new Human(), ElementalType.FOGO).getCriticalEffectImmunities().isEmpty());
    }
}
