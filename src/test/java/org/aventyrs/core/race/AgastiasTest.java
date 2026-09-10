package org.aventyrs.core.race;

import org.aventyrs.core.ability.GnoseAbility;
import org.aventyrs.core.ability.StrengthAbility;
import org.aventyrs.core.action.ActionProfile;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterEgos;
import org.aventyrs.core.character.SizeCategory;
import org.aventyrs.core.feat.FeatCategory;
import org.aventyrs.core.magic.BranchLevel;
import org.aventyrs.core.magic.Spell;
import org.aventyrs.core.magic.TestSpell;
import org.aventyrs.core.magic.TestSpellTree;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Player;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgastiasTest {

    @Test
    void isMestico() {
        assertTrue(new Agastias(new Human(), Agastias.Linhagem.VULCANO).isMestico());
    }

    @Test
    void delegatesCreatureTypeToTheParentRace() {
        assertEquals(CreatureType.HUMANOIDE, new Agastias(new Human(), Agastias.Linhagem.VULCANO).getCreatureType());
        assertEquals(CreatureType.FEERICO, new Agastias(new Fada(), Agastias.Linhagem.VULCANO).getCreatureType());
    }

    @Test
    void vulcanoLinhagemReducesFocus() {
        Agastias agastias = new Agastias(new Human(), Agastias.Linhagem.VULCANO);
        assertEquals(Map.of(AttributeDomain.GNOSE, 2, AttributeDomain.FOCUS, -1), agastias.getFixedAttributeBonuses());
    }

    @Test
    void trovejanteLinhagemReducesStrength() {
        Agastias agastias = new Agastias(new Human(), Agastias.Linhagem.TROVEJANTE);
        assertEquals(Map.of(AttributeDomain.GNOSE, 2, AttributeDomain.STRENGTH, -1), agastias.getFixedAttributeBonuses());
    }

    @Test
    void primaryBonusIncreasesToThreeWhenParentAlsoGrantsGnose() {
        Agastias agastias = new Agastias(new Anao(), Agastias.Linhagem.VULCANO);
        assertEquals(Map.of(AttributeDomain.GNOSE, 3, AttributeDomain.FOCUS, -1), agastias.getFixedAttributeBonuses());
    }

    @Test
    void generateEmptyCharacterInheritsTheParentRacesBaseSizeCategoryWithNoOffset() {
        Agastias agastias = new Agastias(new Anao(), Agastias.Linhagem.VULCANO);
        Character character = agastias.generateEmptyCharacter(List.of())
                .player(new Player())
                .name("Test")
                .race(agastias)
                .attributes(CharacterAttributes.builder().build())
                .egos(CharacterEgos.builder().build())
                .actionProfile(ActionProfile.REFLEXOS_RAPIDOS)
                .build();

        assertEquals(SizeCategory.MINUS_ONE, character.getSizeCategory());
    }

    @Test
    void hasNoInheritedRacialOrAttributeAbilitiesByDefault() {
        Agastias agastias = new Agastias(new Human(), Agastias.Linhagem.VULCANO);
        assertTrue(agastias.getRacialAbilities().isEmpty());
        assertTrue(agastias.getInheritedAttributeAbilities().isEmpty());
    }

    @Test
    void getRacialAbilitiesReturnsTheInheritedAbilitiesFromTheParentRace() {
        Agastias agastias = new Agastias(new Anao(), Agastias.Linhagem.VULCANO,
                List.of(AnoesRacialAbility.ABATEDORES_DE_GIGANTES), List.of());
        assertEquals(List.of(AnoesRacialAbility.ABATEDORES_DE_GIGANTES), agastias.getRacialAbilities());
    }

    @Test
    void usesTheBaseCostsForFeatsAndSkills() {
        Agastias agastias = new Agastias(new Human(), Agastias.Linhagem.VULCANO);
        assertEquals(Race.BASE_NEW_FEAT_COST, agastias.getNewFeatCost(FeatCategory.PERITO));
        assertEquals(Race.BASE_NEW_SKILL_COST, agastias.getNewSkillCost());
    }

    @Test
    void magiaECienciaTakesHalfAnExpOffEveryMagia() {
        Agastias agastias = new Agastias(new Human(), Agastias.Linhagem.VULCANO);
        Spell broto = TestSpell.at(TestSpellTree.LINEAR, BranchLevel.BROTO, null);
        Spell emergente = TestSpell.at(TestSpellTree.LINEAR, BranchLevel.EMERGENTE, null);

        assertEquals(0, new BigDecimal("0.5").compareTo(agastias.resolveSpellAcquisitionCostReduction(null, broto)));
        assertEquals(0, new BigDecimal("0.5").compareTo(agastias.resolveSpellAcquisitionCostReduction(null, emergente)));
    }

    @Test
    void anOrdinaryRaceGivesNoSpellAcquisitionDiscount() {
        assertEquals(0, BigDecimal.ZERO.compareTo(
                new Human().resolveSpellAcquisitionCostReduction(null,
                        TestSpell.at(TestSpellTree.LINEAR, BranchLevel.BROTO, null))));
    }

    @Test
    void constructorRejectsANullParentRace() {
        assertThrows(NullPointerException.class, () -> new Agastias(null, Agastias.Linhagem.VULCANO));
    }

    @Test
    void constructorRejectsANullLinhagem() {
        assertThrows(NullPointerException.class, () -> new Agastias(new Human(), null));
    }

    @Test
    void constructorRejectsAMesticoParentRace() {
        Agastias alreadyMestico = new Agastias(new Human(), Agastias.Linhagem.VULCANO);
        assertThrows(IllegalOperationException.class, () -> new Agastias(alreadyMestico, Agastias.Linhagem.TROVEJANTE));
    }

    @Test
    void constructorRejectsAnInheritedRacialAbilityNotGrantedByTheParentRace() {
        assertThrows(IllegalOperationException.class,
                () -> new Agastias(new Anao(), Agastias.Linhagem.VULCANO,
                        List.of(ElfosRacialAbility.SENTIDOS_ABSOLUTOS), List.of()));
    }

    @Test
    void constructorRejectsAnInheritedAttributeAbilityForAnAttributeTheParentDoesNotGrant() {
        assertThrows(IllegalOperationException.class,
                () -> new Agastias(new Human(), Agastias.Linhagem.VULCANO, List.of(), List.of(GnoseAbility.DOMINIO_DO_CONHECIMENTO)));
    }

    @Test
    void constructorAcceptsOneInheritedAttributeAbilityPerAttributeTheParentGrants() {
        Agastias agastias = new Agastias(new Anao(), Agastias.Linhagem.VULCANO, List.of(), List.of(GnoseAbility.DOMINIO_DO_CONHECIMENTO));
        assertEquals(List.of(GnoseAbility.DOMINIO_DO_CONHECIMENTO), agastias.getInheritedAttributeAbilities());
    }

    @Test
    void strengthAbilityIsUnrelatedToWhatAnaoGrantsAndIsThereforeRejected() {
        assertThrows(IllegalOperationException.class,
                () -> new Agastias(new Anao(), Agastias.Linhagem.VULCANO, List.of(), List.of(StrengthAbility.SUBJUGAR)));
    }
}
