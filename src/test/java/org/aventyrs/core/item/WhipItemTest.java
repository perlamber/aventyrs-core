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
import static org.junit.jupiter.api.Assertions.assertTrue;

class WhipItemTest {

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    @Test
    void hasOneConstantPerCatalogedChicote() {
        assertEquals(4, WhipItem.values().length);
    }

    @Test
    void everyConstantIsANamedMeleeOffensiveWeapon() {
        Arrays.stream(WhipItem.values()).forEach(whip -> {
            assertEquals(ItemCategory.WHIP, whip.getCategory());
            assertEquals(ItemType.OFFENSIVE, whip.getType());
            assertFalse(whip.getName().isBlank());
            assertEquals(SkillType.ATAQUE_CORPO_A_CORPO, whip.getSkillType());
            assertEquals(0, whip.getHardness());
            assertEquals(0, whip.getPhysicalDefenseBonus());
            assertEquals(0, whip.getCastingBonus());
        });
    }

    @Test
    void chicoteCarriesEveryColumnOfItsRulesText() {
        WhipItem whip = WhipItem.CHICOTE;

        assertEquals("Chicote", whip.getName());
        assertEquals(ItemWeightClass.LIGHT, whip.getWeightClass());
        assertEquals(ItemRarity.UNCOMMON, whip.getRarity());
        assertEquals(11, whip.getPrice());
        assertEquals(DamageBase.of(1, 1), whip.getDamageBase());
        assertEquals(Range.DISTANCIA_CURTA, whip.getRange());
        assertEquals(CriticalEffectType.ATORDOANTE, whip.getCriticalEffect());
        assertEquals(16, whip.getLesserCriticalMargin());
    }

    @Test
    void correnteEspinhosaAndEspadaChicoteCarryTheirOwnCritColumns() {
        assertEquals(CriticalEffectType.DESMEMBRAR, WhipItem.CORRENTE_ESPINHOSA.getCriticalEffect());
        assertEquals(16, WhipItem.CORRENTE_ESPINHOSA.getLesserCriticalMargin());
        assertEquals(DamageBase.of(2, 0), WhipItem.CORRENTE_ESPINHOSA.getDamageBase());
        assertEquals(Range.DISTANCIA_MUITO_CURTA, WhipItem.CORRENTE_ESPINHOSA.getRange());

        assertEquals(CriticalEffectType.GUILHOTINA, WhipItem.ESPADA_CHICOTE.getCriticalEffect());
        assertEquals(17, WhipItem.ESPADA_CHICOTE.getLesserCriticalMargin());
        assertEquals(ItemRarity.MYTHIC, WhipItem.ESPADA_CHICOTE.getRarity());
        assertEquals(Range.ADJACENTE, WhipItem.ESPADA_CHICOTE.getRange());
    }

    /** Every Chicote Favor is a Corrente de Efeitos — prose only, no {@link ItemBonus}. */
    @Test
    void noChicoteFavorGrantsARealBonus() {
        Arrays.stream(WhipItem.values()).forEach(whip -> {
            assertTrue(whip.getFavor().getBonuses().isEmpty(), whip.getName());
            assertTrue(whip.getFavor().getDescription().contains("Corrente de Efeitos"), whip.getName());
        });
    }
}
