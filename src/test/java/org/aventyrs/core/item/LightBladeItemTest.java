package org.aventyrs.core.item;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.DamageBase;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.effect.CriticalEffectType;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LightBladeItemTest {

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    @Test
    void hasOneConstantPerCatalogedLaminaLeve() {
        assertEquals(6, LightBladeItem.values().length);
    }

    @Test
    void everyConstantIsANamedAdjacentMeleeOffensiveWeapon() {
        Arrays.stream(LightBladeItem.values()).forEach(blade -> {
            assertEquals(ItemCategory.LIGHT_BLADE, blade.getCategory());
            assertEquals(ItemType.OFFENSIVE, blade.getType());
            assertFalse(blade.getName().isBlank());
            assertEquals(SkillType.ATAQUE_CORPO_A_CORPO, blade.getSkillType());
            assertEquals(Range.ADJACENTE, blade.getRange());
            assertEquals(17, blade.getLesserCriticalMargin(), blade.getName());
            assertEquals(0, blade.getHardness());
            assertEquals(0, blade.getPhysicalDefenseBonus());
            assertEquals(0, blade.getCastingBonus());
        });
    }

    @Test
    void adagaCarriesEveryColumnOfItsRulesTextAndNoFavor() {
        LightBladeItem adaga = LightBladeItem.ADAGA_KUNAI_OU_SEAX;

        assertEquals("Adaga, Kunai ou Seax", adaga.getName());
        assertEquals(ItemWeightClass.LIGHT, adaga.getWeightClass());
        assertEquals(ItemRarity.COMMON, adaga.getRarity());
        assertEquals(3, adaga.getPrice());
        assertEquals(DamageBase.of(1, 1), adaga.getDamageBase());
        assertEquals(CriticalEffectType.SANGRAMENTO, adaga.getCriticalEffect());
        assertNull(adaga.getFavor());
    }

    @Test
    void machadoDeMaoAndEspadaCurtaCarryNoFavor() {
        assertNull(LightBladeItem.MACHADO_DE_MAO.getFavor());
        assertNull(LightBladeItem.ESPADA_CURTA_OU_WAKIZASHI.getFavor());
        assertEquals(CriticalEffectType.DILACERAR, LightBladeItem.MACHADO_DE_MAO.getCriticalEffect());
    }

    @Test
    void espadaGanchoFavorGrantsAnUnconditionalPlusOnePhysicalDefense() {
        LightBladeItem gancho = LightBladeItem.ESPADA_GANCHO_OU_SAI;
        Character anyone = CharacterFixture.blank(CharacterFixture.BLANK).build();

        assertEquals(1, gancho.resolveFavorBonus(ModifierType.PHYSICAL_DEFENSE, anyone));
        assertTrue(gancho.getFavor().getDescription().contains("Prender a Arma"));
    }

    /** The "Margem Crítica Menor muda para 16" Favores stay prose — the table margin (17) stands. */
    @Test
    void floreteAndFoiceDeMaoKeepTheirTableMargin() {
        assertEquals(17, LightBladeItem.FLORETE_OU_SABRE.getLesserCriticalMargin());
        assertEquals(17, LightBladeItem.FOICE_DE_MAO.getLesserCriticalMargin());
        assertTrue(LightBladeItem.FLORETE_OU_SABRE.getFavor().getBonuses().isEmpty());
        assertTrue(LightBladeItem.FOICE_DE_MAO.getFavor().getBonuses().isEmpty());
    }
}
