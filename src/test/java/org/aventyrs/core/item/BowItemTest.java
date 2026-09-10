package org.aventyrs.core.item;

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

class BowItemTest {

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    @Test
    void hasOneConstantPerCatalogedArco() {
        assertEquals(3, BowItem.values().length);
    }

    @Test
    void everyConstantIsANamedOffensiveWeaponWithNoDefensiveOrConjuracaoColumn() {
        Arrays.stream(BowItem.values()).forEach(bow -> {
            assertEquals(ItemCategory.BOW, bow.getCategory());
            assertEquals(ItemType.OFFENSIVE, bow.getType());
            assertFalse(bow.getName().isBlank());
            assertEquals(SkillType.ATAQUE_A_DISTANCIA, bow.getSkillType());
            assertEquals(bow.getSkillType(), bow.getAttackSkillType());
            assertFalse(bow.getDamageBase() == null, bow.getName());
            assertEquals(0, bow.getPhysicalDefenseBonus());
            assertEquals(0, bow.getMagicDefenseBonus());
            assertEquals(0, bow.getHardness());
            assertEquals(0, bow.getCastingBonus());
            assertFalse(bow.isDestroyed());
        });
    }

    @Test
    void everyArcoLeavesTheProjetilCritColumnNullAtTheDefaultMargin() {
        Arrays.stream(BowItem.values()).forEach(bow -> {
            assertNull(bow.getCriticalEffect(), bow.getName());
            assertEquals(Weapon.DEFAULT_LESSER_CRITICAL_MARGIN, bow.getLesserCriticalMargin());
        });
    }

    @Test
    void arcoCompostoCarriesEveryColumnOfItsRulesText() {
        BowItem bow = BowItem.ARCO_COMPOSTO;

        assertEquals("Arco Composto", bow.getName());
        assertEquals(ItemWeightClass.MEDIUM, bow.getWeightClass());
        assertEquals(ItemRarity.RARE, bow.getRarity());
        assertEquals(17, bow.getPrice());
        assertEquals(DamageBase.of(1, 2), bow.getDamageBase());
        assertEquals(Range.DISTANCIA_LONGA, bow.getRange());
    }

    @Test
    void arcoCurtoCarriesEveryColumnOfItsRulesText() {
        BowItem bow = BowItem.ARCO_CURTO;

        assertEquals("Arco Curto", bow.getName());
        assertEquals(ItemWeightClass.LIGHT, bow.getWeightClass());
        assertEquals(ItemRarity.COMMON, bow.getRarity());
        assertEquals(9, bow.getPrice());
        assertEquals(DamageBase.of(1, 1), bow.getDamageBase());
        assertEquals(Range.DISTANCIA_MEDIA, bow.getRange());
    }

    @Test
    void arcoLongoCarriesEveryColumnOfItsRulesText() {
        BowItem bow = BowItem.ARCO_LONGO;

        assertEquals("Arco Longo", bow.getName());
        assertEquals(ItemWeightClass.HEAVY, bow.getWeightClass());
        assertEquals(ItemRarity.UNCOMMON, bow.getRarity());
        assertEquals(14, bow.getPrice());
        assertEquals(DamageBase.of(1, 3), bow.getDamageBase());
        assertEquals(Range.DISTANCIA_LONGA, bow.getRange());
    }

    /** Every Arco Favor is unexpressible rules text — none contributes an {@link ItemBonus}. */
    @Test
    void noArcoFavorGrantsARealBonus() {
        Arrays.stream(BowItem.values()).forEach(bow -> {
            assertTrue(bow.getFavor().getBonuses().isEmpty(), bow.getName());
            assertFalse(bow.getFavor().getDescription().isBlank(), bow.getName());
        });
    }

    @Test
    void arcoLongoKeepsItsTableRangeDespiteItsMudaParaFavor() {
        assertEquals(Range.DISTANCIA_LONGA, BowItem.ARCO_LONGO.getRange());
        assertTrue(BowItem.ARCO_LONGO.getFavor().getDescription().contains("Muito Longa"));
    }
}
