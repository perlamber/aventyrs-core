package org.aventyrs.core.item;

import org.aventyrs.core.character.DamageBase;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.effect.CriticalEffectType;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThrowableItemTest {

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    @Test
    void hasOneConstantPerCatalogedArremesso() {
        assertEquals(4, ThrowableItem.values().length);
    }

    @Test
    void everyConstantIsANamedRangedOffensiveWeapon() {
        Arrays.stream(ThrowableItem.values()).forEach(weapon -> {
            assertEquals(ItemCategory.THROWABLE, weapon.getCategory());
            assertEquals(ItemType.OFFENSIVE, weapon.getType());
            assertFalse(weapon.getName().isBlank());
            assertEquals(SkillType.ATAQUE_A_DISTANCIA, weapon.getSkillType());
            assertNotNull(weapon.getDamageBase());
            assertEquals(0, weapon.getHardness());
            assertEquals(0, weapon.getPhysicalDefenseBonus());
            assertEquals(0, weapon.getMagicDefenseBonus());
            assertEquals(0, weapon.getCastingBonus());
        });
    }

    @Test
    void dardosCarryExcruciante() {
        ThrowableItem dardos = ThrowableItem.DARDOS_E_SHUKENS;

        assertEquals("Dardos e Shukens", dardos.getName());
        assertEquals(ItemWeightClass.LIGHT, dardos.getWeightClass());
        assertEquals(ItemRarity.COMMON, dardos.getRarity());
        assertEquals(7, dardos.getPrice());
        assertEquals(DamageBase.of(1, 1), dardos.getDamageBase());
        assertEquals(Range.DISTANCIA_CURTA, dardos.getRange());
        assertEquals(CriticalEffectType.EXCRUCIANTE, dardos.getCriticalEffect());
        assertEquals(17, dardos.getLesserCriticalMargin());
    }

    @Test
    void pilumCarriesEstilhacadorAndIsTwoDice() {
        ThrowableItem pilum = ThrowableItem.PILUM;

        assertEquals(DamageBase.of(2, 0), pilum.getDamageBase());
        assertEquals(ItemRarity.RARE, pilum.getRarity());
        assertEquals(14, pilum.getPrice());
        assertEquals(CriticalEffectType.ESTILHACADOR, pilum.getCriticalEffect());
    }

    /**
     * The Zarabatanas print the defective {@code Projétil | Projétil | Projétil} Dano row — their
     * dano is reconstructed from the prose (a Dardo raised +1 / +2) and their crit column is not
     * a catalogued {@link CriticalEffectType}.
     */
    @Test
    void zarabatanasReconstructTheirDanoFromProseAndCarryNoCritEffect() {
        assertEquals(DamageBase.of(1, 2), ThrowableItem.ZARABATANA.getDamageBase());
        assertEquals(DamageBase.of(1, 3), ThrowableItem.ZARABATANA_DE_CACA.getDamageBase());
        assertNull(ThrowableItem.ZARABATANA.getCriticalEffect());
        assertNull(ThrowableItem.ZARABATANA_DE_CACA.getCriticalEffect());
        assertEquals(Range.DISTANCIA_MEDIA, ThrowableItem.ZARABATANA.getRange());
        assertEquals(Range.DISTANCIA_LONGA, ThrowableItem.ZARABATANA_DE_CACA.getRange());
    }

    @Test
    void noArremessoFavorGrantsARealBonus() {
        Arrays.stream(ThrowableItem.values()).forEach(weapon ->
                assertTrue(weapon.getFavor().getBonuses().isEmpty(), weapon.getName()));
    }

    @Test
    void everyArremessoFavorIsDescribedAndCarriesItsAdditionalEffects() {
        Arrays.stream(ThrowableItem.values()).forEach(weapon -> {
            assertFalse(weapon.getFavor().getDescription().isBlank(), weapon.getName());
            assertTrue(weapon.getFavor().hasAdditionalEffects(), weapon.getName());
        });
    }
}
