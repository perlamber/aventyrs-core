package org.aventyrs.core.item;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.DamageBase;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrossbowItemTest {

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    @Test
    void hasOneConstantPerCatalogedBalestra() {
        assertEquals(3, CrossbowItem.values().length);
    }

    @Test
    void everyConstantIsANamedRangedOffensiveWeaponAtMargin16() {
        Arrays.stream(CrossbowItem.values()).forEach(besta -> {
            assertEquals(ItemCategory.CROSSBOW, besta.getCategory());
            assertEquals(ItemType.OFFENSIVE, besta.getType());
            assertFalse(besta.getName().isBlank());
            assertEquals(SkillType.ATAQUE_A_DISTANCIA, besta.getSkillType());
            assertNull(besta.getCriticalEffect(), besta.getName());
            assertEquals(16, besta.getLesserCriticalMargin());
            assertEquals(0, besta.getHardness());
            assertEquals(0, besta.getCastingBonus());
        });
    }

    @Test
    void bestaDeMaoCarriesEveryColumnOfItsRulesText() {
        CrossbowItem besta = CrossbowItem.BESTA_DE_MAO;

        assertEquals("Besta de Mão", besta.getName());
        assertEquals(ItemWeightClass.LIGHT, besta.getWeightClass());
        assertEquals(ItemRarity.RARE, besta.getRarity());
        assertEquals(17, besta.getPrice());
        assertEquals(DamageBase.of(1, 3), besta.getDamageBase());
        assertEquals(Range.DISTANCIA_MEDIA, besta.getRange());
    }

    @Test
    void bestaDeRepeticaoIsMythic() {
        assertEquals(ItemRarity.MYTHIC, CrossbowItem.BESTA_DE_REPETICAO.getRarity());
        assertEquals(DamageBase.of(1, 2), CrossbowItem.BESTA_DE_REPETICAO.getDamageBase());
    }

    @Test
    void bestaPesadaFavorGrantsRangedAttackAdvantageOnlyWhenTheRequirementIsMet() {
        CrossbowItem besta = CrossbowItem.BESTA_PESADA;

        assertEquals(DamageBase.of(2, 1), besta.getDamageBase());
        assertEquals(Range.DISTANCIA_LONGA, besta.getRange());
        assertEquals(Skill.ADVANTAGE_BONUS, besta.resolveFavorBonus(
                ModifierType.ATAQUE_A_DISTANCIA_ROLL_BONUS, characterWith(AttributeDomain.DEXTERITY, 3)));
        assertEquals(Skill.ADVANTAGE_BONUS, besta.resolveFavorBonus(
                ModifierType.ATAQUE_A_DISTANCIA_ROLL_BONUS, characterWith(AttributeDomain.STRENGTH, 3)));
        assertEquals(0, besta.resolveFavorBonus(
                ModifierType.ATAQUE_A_DISTANCIA_ROLL_BONUS, characterWith(AttributeDomain.DEXTERITY, 2)));
    }

    @Test
    void theTwoReloadFavoresGrantNoRealBonus() {
        assertTrue(CrossbowItem.BESTA_DE_MAO.getFavor().getBonuses().isEmpty());
        assertTrue(CrossbowItem.BESTA_DE_REPETICAO.getFavor().getBonuses().isEmpty());
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
