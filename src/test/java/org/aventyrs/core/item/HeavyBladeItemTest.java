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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeavyBladeItemTest {

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    @Test
    void hasOneConstantPerCatalogedLaminaPesada() {
        assertEquals(6, HeavyBladeItem.values().length);
    }

    @Test
    void everyConstantIsANamedAdjacentMeleeOffensiveWeaponAtMargin17() {
        Arrays.stream(HeavyBladeItem.values()).forEach(blade -> {
            assertEquals(ItemCategory.HEAVY_BLADE, blade.getCategory());
            assertEquals(ItemType.OFFENSIVE, blade.getType());
            assertFalse(blade.getName().isBlank());
            assertEquals(SkillType.ATAQUE_CORPO_A_CORPO, blade.getSkillType());
            assertEquals(Range.ADJACENTE, blade.getRange());
            assertEquals(17, blade.getLesserCriticalMargin(), blade.getName());
            assertEquals(0, blade.getHardness());
            assertEquals(0, blade.getCastingBonus());
        });
    }

    @Test
    void espadaLongaCarriesEveryColumnOfItsRulesText() {
        HeavyBladeItem blade = HeavyBladeItem.ESPADA_LONGA_OU_KATANA;

        assertEquals("Espada Longa ou Katana", blade.getName());
        assertEquals(ItemWeightClass.HEAVY, blade.getWeightClass());
        assertEquals(ItemRarity.UNCOMMON, blade.getRarity());
        assertEquals(10, blade.getPrice());
        assertEquals(DamageBase.of(1, 3), blade.getDamageBase());
        assertEquals(CriticalEffectType.DESMEMBRAR, blade.getCriticalEffect());
    }

    @Test
    void mataDragaoIsAThreeDiceMythic() {
        assertEquals(DamageBase.of(3, 0), HeavyBladeItem.MATA_DRAGAO.getDamageBase());
        assertEquals(ItemRarity.MYTHIC, HeavyBladeItem.MATA_DRAGAO.getRarity());
        assertFalse(HeavyBladeItem.MATA_DRAGAO.getDescription().isBlank());
    }

    @Test
    void espadaBastardaCarriesNoFavor() {
        assertNull(HeavyBladeItem.ESPADA_BASTARDA_OU_KODACHI.getFavor());
    }

    /** Every other Favor is a "muda para" / crit-dano clause — prose only. */
    @Test
    void noHeavyBladeFavorGrantsARealBonus() {
        Arrays.stream(HeavyBladeItem.values())
                .filter(blade -> blade.getFavor() != null)
                .forEach(blade -> assertTrue(blade.getFavor().getBonuses().isEmpty(), blade.getName()));
    }
}
