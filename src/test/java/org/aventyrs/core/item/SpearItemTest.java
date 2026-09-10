package org.aventyrs.core.item;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.DamageBase;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.effect.CriticalEffectType;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpearItemTest {

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    @Test
    void hasOneConstantPerCatalogedLanca() {
        assertEquals(5, SpearItem.values().length);
    }

    @Test
    void everyConstantIsANamedMeleeOffensiveWeapon() {
        Arrays.stream(SpearItem.values()).forEach(spear -> {
            assertEquals(ItemCategory.SPEAR, spear.getCategory());
            assertEquals(ItemType.OFFENSIVE, spear.getType());
            assertFalse(spear.getName().isBlank());
            assertEquals(SkillType.ATAQUE_CORPO_A_CORPO, spear.getSkillType());
            assertEquals(17, spear.getLesserCriticalMargin(), spear.getName());
            assertEquals(0, spear.getHardness());
            assertEquals(0, spear.getCastingBonus());
        });
    }

    @Test
    void lancaCarriesEveryColumnOfItsRulesText() {
        SpearItem lanca = SpearItem.LANCA;

        assertEquals("Lança", lanca.getName());
        assertEquals(ItemWeightClass.MEDIUM, lanca.getWeightClass());
        assertEquals(ItemRarity.COMMON, lanca.getRarity());
        assertEquals(8, lanca.getPrice());
        assertEquals(DamageBase.of(1, 3), lanca.getDamageBase());
        assertEquals(Range.DISTANCIA_MUITO_CURTA, lanca.getRange());
        assertEquals(CriticalEffectType.EMPALAR, lanca.getCriticalEffect());
    }

    @Test
    void reachVariesPerConstant() {
        assertEquals(Range.ADJACENTE, SpearItem.ALABARDA_OU_NAGINATA.getRange());
        assertEquals(Range.DISTANCIA_MUITO_CURTA, SpearItem.JAVELIN.getRange());
        assertEquals(Range.DISTANCIA_CURTA, SpearItem.PIQUE.getRange());
    }

    @Test
    void javelinFavorGrantsRangedAttackAdvantageOnlyWhenTheRequirementIsMet() {
        SpearItem javelin = SpearItem.JAVELIN;

        assertEquals(Skill.ADVANTAGE_BONUS, javelin.resolveFavorBonus(
                ModifierType.ATAQUE_A_DISTANCIA_ROLL_BONUS, characterWith(AttributeDomain.STRENGTH, 3)));
        assertEquals(Skill.ADVANTAGE_BONUS, javelin.resolveFavorBonus(
                ModifierType.ATAQUE_A_DISTANCIA_ROLL_BONUS, characterWith(AttributeDomain.DEXTERITY, 3)));
        assertEquals(0, javelin.resolveFavorBonus(
                ModifierType.ATAQUE_A_DISTANCIA_ROLL_BONUS, characterWith(AttributeDomain.STRENGTH, 2)));
    }

    @Test
    void everyOtherLancaFavorGrantsNoRealBonus() {
        Arrays.stream(SpearItem.values())
                .filter(spear -> spear != SpearItem.JAVELIN)
                .forEach(spear -> assertTrue(spear.getFavor().getBonuses().isEmpty(), spear.getName()));
    }

    private static Character characterWith(final AttributeDomain domain, final int base) {
        AttributeValue value = AttributeValue.builder().domain(domain).base(base).build();
        CharacterAttributes.CharacterAttributesBuilder attributes = CharacterAttributes.builder();
        switch (domain) {
            case STRENGTH -> attributes.strength(value);
            case DEXTERITY -> attributes.dexterity(value);
            default -> throw new IllegalArgumentException("Unsupported test domain: " + domain);
        }
        return CharacterFixture.blank(CharacterFixture.BLANK).attributes(attributes.build()).build();
    }
}
