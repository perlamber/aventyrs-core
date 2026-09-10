package org.aventyrs.core.item;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.DamageBase;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NaturalWeaponTest {

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    @Test
    void hasOneConstantPerAuthoredArmaNatural() {
        assertEquals(7, NaturalWeapon.values().length);
    }

    @Test
    void everyConstantIsANamedNaturalWeaponWithNoEconomyAndNoFavor() {
        Arrays.stream(NaturalWeapon.values()).forEach(weapon -> {
            assertEquals(ItemCategory.NATURAL_WEAPON, weapon.getCategory());
            assertEquals(ItemType.OFFENSIVE, weapon.getType());
            assertEquals(ItemRarity.NATURAL, weapon.getRarity());
            assertFalse(weapon.getName().isBlank());
            assertEquals(0, weapon.getPrice());
            assertEquals(0, weapon.getPhysicalDefenseBonus());
            assertEquals(0, weapon.getMagicDefenseBonus());
            assertEquals(0, weapon.getHardness());
            assertEquals(0, weapon.getCastingBonus());
            assertNull(weapon.getFavor());
            assertFalse(weapon.isDestroyed());
        });
    }

    @Test
    void everyConstantCarriesADamageBaseAndAnAttackPericia() {
        Arrays.stream(NaturalWeapon.values()).forEach(weapon -> {
            assertFalse(weapon.getDamageBase() == null, weapon.getName());
            assertTrue(weapon.getSkillType().isAttackSkill(), weapon.getName());
            assertEquals(weapon.getSkillType(), weapon.getAttackSkillType());
        });
    }

    @Test
    void anyCharacterTreatsAnActualNaturalWeaponAsNatural() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();
        Arrays.stream(NaturalWeapon.values())
                .forEach(weapon -> assertTrue(character.treatsAsNaturalWeapon(weapon), weapon.getName()));
    }

    @Test
    void armaDeSoproCarriesEveryColumnOfItsRulesText() {
        NaturalWeapon weapon = NaturalWeapon.ARMA_DE_SOPRO;

        assertEquals("Arma de Sopro", weapon.getName());
        assertEquals(ItemWeightClass.LIGHT, weapon.getWeightClass());
        assertEquals(DamageBase.of(1, 2), weapon.getDamageBase());
        assertEquals(SkillType.ATAQUE_A_DISTANCIA, weapon.getSkillType());
        assertEquals(Range.DISTANCIA_MEDIA, weapon.getRange());
    }

    @Test
    void chifresPoderososCarriesEveryColumnOfItsRulesText() {
        NaturalWeapon weapon = NaturalWeapon.CHIFRES_PODEROSOS;

        assertEquals("Chifres Poderosos", weapon.getName());
        assertEquals(ItemWeightClass.MEDIUM, weapon.getWeightClass());
        assertEquals(DamageBase.of(1, 1), weapon.getDamageBase());
        assertEquals(SkillType.ATAQUE_CORPO_A_CORPO, weapon.getSkillType());
        assertEquals(Range.ADJACENTE, weapon.getRange());
    }

    @Test
    void garrasAfiadasCarriesEveryColumnOfItsRulesText() {
        NaturalWeapon weapon = NaturalWeapon.GARRAS_AFIADAS;

        assertEquals("Garras Afiadas", weapon.getName());
        assertEquals(ItemWeightClass.LIGHT, weapon.getWeightClass());
        assertEquals(DamageBase.of(1, 1), weapon.getDamageBase());
        assertEquals(SkillType.ATAQUE_CORPO_A_CORPO, weapon.getSkillType());
    }

    @Test
    void presasLongasCarriesEveryColumnOfItsRulesText() {
        NaturalWeapon weapon = NaturalWeapon.PRESAS_LONGAS;

        assertEquals("Presas Longas", weapon.getName());
        assertEquals(ItemWeightClass.LIGHT, weapon.getWeightClass());
        assertEquals(DamageBase.of(1, 1), weapon.getDamageBase());
        assertEquals(SkillType.ATAQUE_CORPO_A_CORPO, weapon.getSkillType());
    }

    @Test
    void caudaChicoteAndConstritoraDifferOnlyInWeight() {
        assertEquals(ItemWeightClass.MEDIUM, NaturalWeapon.CAUDA_CHICOTE.getWeightClass());
        assertEquals(ItemWeightClass.HEAVY, NaturalWeapon.CAUDA_CONSTRITORA.getWeightClass());
        assertEquals(NaturalWeapon.CAUDA_CHICOTE.getDamageBase(), NaturalWeapon.CAUDA_CONSTRITORA.getDamageBase());
    }

    @Test
    void ataqueDesarmadoIsTheBottomRungOfTheScale() {
        assertEquals(DamageBase.UNARMED, NaturalWeapon.ATAQUE_DESARMADO.getDamageBase());
        assertEquals(SkillType.ATAQUE_CORPO_A_CORPO, NaturalWeapon.ATAQUE_DESARMADO.getSkillType());
    }
}
