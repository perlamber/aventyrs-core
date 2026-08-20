package org.aventyrs.core.item;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.skill.Skill;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArmorItemTest {

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    @Test
    void hasOneConstantPerCatalogedArmor() {
        assertEquals(3, ArmorItem.values().length);
    }

    @Test
    void everyConstantIsANamedArmor() {
        Arrays.stream(ArmorItem.values()).forEach(armor -> {
            assertEquals(ItemCategory.ARMOR, armor.getCategory());
            assertEquals(ItemType.DEFENSIVE, armor.getType());
            assertFalse(armor.getName().isBlank());
        });
    }

    @Test
    void armaduraCompletaCarriesEveryColumnOfItsRulesText() {
        ArmorItem armor = ArmorItem.ARMADURA_COMPLETA;

        assertEquals("Armadura Completa", armor.getName());
        assertEquals(ItemWeightClass.HEAVY, armor.getWeightClass());
        assertEquals(ItemRarity.RARE, armor.getRarity());
        assertEquals(20, armor.getPrice());
        assertEquals(5, armor.getPhysicalDefenseBonus());
        assertEquals(2, armor.getMagicDefenseBonus());
        assertEquals(32, armor.getHardness());
    }

    @Test
    void armaduraCompletaHindersConjuracaoByAFlatDesvantagem() {
        assertEquals(Skill.DISADVANTAGE_MALUS, ArmorItem.ARMADURA_COMPLETA.getCastingBonus());
        assertEquals(-Skill.ADVANTAGE_BONUS, ArmorItem.ARMADURA_COMPLETA.getCastingBonus());
    }

    @Test
    void armaduraCompletaGrantsItsFavorOnlyAtForca3() {
        assertTrue(ArmorItem.ARMADURA_COMPLETA.grantsFavorTo(characterWithStrengthBase(3)));
        assertFalse(ArmorItem.ARMADURA_COMPLETA.grantsFavorTo(characterWithStrengthBase(2)));
    }

    @Test
    void armaduraCompletaFavorGrantsRealDamageReductionOnlyAtForca3() {
        assertEquals(2, ArmorItem.ARMADURA_COMPLETA
                .resolveFavorBonus(ModifierType.DAMAGE_REDUCTION, characterWithStrengthBase(3)));
        assertEquals(0, ArmorItem.ARMADURA_COMPLETA
                .resolveFavorBonus(ModifierType.DAMAGE_REDUCTION, characterWithStrengthBase(2)));
    }

    @Test
    void armaduraCompletaFavorGrantsNothingItDoesNotName() {
        assertEquals(0, ArmorItem.ARMADURA_COMPLETA
                .resolveFavorBonus(ModifierType.DEFESAS, characterWithStrengthBase(3)));
    }

    @Test
    void everyCatalogedFavorIsDescribedAndGrantsAtLeastOneRealBonus() {
        Arrays.stream(ArmorItem.values())
                .map(ArmorItem::getFavor)
                .filter(Objects::nonNull)
                .forEach(favor -> {
                    assertFalse(favor.getDescription().isBlank());
                    assertFalse(favor.getBonuses().isEmpty());
                });
    }

    @Test
    void armaduraDeGladiadorCarriesEveryColumnOfItsRulesText() {
        ArmorItem armor = ArmorItem.ARMADURA_DE_GLADIADOR;

        assertEquals("Armadura de Gladiador", armor.getName());
        assertEquals(ItemWeightClass.LIGHT, armor.getWeightClass());
        assertEquals(ItemRarity.COMMON, armor.getRarity());
        assertEquals(5, armor.getPrice());
        assertEquals(2, armor.getPhysicalDefenseBonus());
        assertEquals(1, armor.getMagicDefenseBonus());
        assertEquals(24, armor.getHardness());
    }

    @Test
    void armaduraDeGladiadorNeitherHelpsNorHindersConjuracao() {
        assertEquals(0, ArmorItem.ARMADURA_DE_GLADIADOR.getCastingBonus());
    }

    @Test
    void armaduraDeGladiadorGrantsNoFavorAtAll() {
        ArmorItem armor = ArmorItem.ARMADURA_DE_GLADIADOR;
        Character character = characterWithStrengthBase(5);

        assertNull(armor.getFavor());
        assertFalse(armor.grantsFavorTo(character));
        assertEquals(0, armor.resolveFavorBonus(ModifierType.DAMAGE_REDUCTION, character));
        assertEquals(List.of(), armor.resolveFavorBonuses(character));
    }

    @Test
    void armaduraDeJustaCarriesEveryColumnOfItsRulesText() {
        ArmorItem armor = ArmorItem.ARMADURA_DE_JUSTA;

        assertEquals("Armadura de Justa", armor.getName());
        assertEquals(ItemWeightClass.HEAVY, armor.getWeightClass());
        assertEquals(ItemRarity.EPIC, armor.getRarity());
        assertEquals(24, armor.getPrice());
        assertEquals(4, armor.getPhysicalDefenseBonus());
        assertEquals(4, armor.getMagicDefenseBonus());
        assertEquals(35, armor.getHardness());
        assertEquals(Skill.DISADVANTAGE_MALUS, armor.getCastingBonus());
    }

    @Test
    void armaduraDeJustaFavorGrantsRealDamageReductionOnlyAtForca4() {
        assertEquals(2, ArmorItem.ARMADURA_DE_JUSTA
                .resolveFavorBonus(ModifierType.DAMAGE_REDUCTION, characterWithStrengthBase(4)));
        assertEquals(0, ArmorItem.ARMADURA_DE_JUSTA
                .resolveFavorBonus(ModifierType.DAMAGE_REDUCTION, characterWithStrengthBase(3)));
    }

    /**
     * The Favor's "Movimento Base reduzido à metade" half is deliberately not an {@link
     * ItemBonus} — halving isn't expressible as a flat {@link ModifierType} value, and no
     * multiplicative step exists in {@code MovementService} to feed one anyway. It stays as
     * rules text on the Favor's own description, which this asserts both halves of.
     */
    @Test
    void armaduraDeJustaGrantsNoMovementBonusForItsHalvingClause() {
        Character character = characterWithStrengthBase(4);

        assertEquals(0, ArmorItem.ARMADURA_DE_JUSTA
                .resolveFavorBonus(ModifierType.MOVEMENT, character));
        assertEquals(List.of(new ItemBonus(ModifierType.DAMAGE_REDUCTION, 2)),
                ArmorItem.ARMADURA_DE_JUSTA.resolveFavorBonuses(character));
        assertTrue(ArmorItem.ARMADURA_DE_JUSTA.getFavor().getDescription()
                .contains("Movimento Base reduzido à metade"));
    }

    @Test
    void armaduraDeJustaHasNoAdditionalEffects() {
        assertFalse(ArmorItem.ARMADURA_DE_JUSTA.getFavor().hasAdditionalEffects());
    }

    @Test
    void armaduraCompletaHasNoAdditionalEffects() {
        assertFalse(ArmorItem.ARMADURA_COMPLETA.getFavor().hasAdditionalEffects());
    }

    private static Character characterWithStrengthBase(final int base) {
        return CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.builder()
                        .strength(AttributeValue.builder().domain(AttributeDomain.STRENGTH).base(base).build())
                        .build())
                .build();
    }
}
