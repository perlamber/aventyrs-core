package org.aventyrs.core.item;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.modifier.ModifierType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemFavorTest {

    private static final ItemRequirements FORCA_3 = new ItemRequirements(AttributeDomain.STRENGTH, 3);

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    @Test
    void carriesNoAdditionalEffectsWhenBuiltWithoutThem() {
        ItemFavor favor = ItemFavor.builder().description("Favor").requirements(FORCA_3).build();

        assertNull(favor.getAdditionalEffects());
        assertFalse(favor.hasAdditionalEffects());
    }

    @Test
    void reportsItsAdditionalEffectsWhenGivenSome() {
        ItemFavor favor = ItemFavor.builder()
                .description("Favor")
                .requirements(FORCA_3)
                .additionalEffects("Efeito adicional")
                .build();

        assertEquals("Efeito adicional", favor.getAdditionalEffects());
        assertTrue(favor.hasAdditionalEffects());
    }

    @Test
    void grantsNoBonusesWhenBuiltWithNone() {
        ItemFavor favor = ItemFavor.builder().description("Favor").build();

        assertEquals(List.of(), favor.getBonuses());
        assertEquals(0, favor.resolveBonus(ModifierType.DAMAGE_REDUCTION, characterWithStrengthBase(5)));
    }

    @Test
    void isGrantedOnlyToACharacterMeetingItsRequirements() {
        ItemFavor favor = ItemFavor.builder().description("Favor").requirements(FORCA_3).build();

        assertTrue(favor.isGrantedTo(characterWithStrengthBase(3)));
        assertFalse(favor.isGrantedTo(characterWithStrengthBase(2)));
    }

    @Test
    void isGrantedUnconditionallyWhenItCarriesNoRequirements() {
        ItemFavor favor = ItemFavor.builder().description("Favor").build();

        assertTrue(favor.isGrantedTo(characterWithStrengthBase(1)));
    }

    @Test
    void resolvesItsBonusOnlyForACharacterMeetingItsRequirements() {
        ItemFavor favor = damageReductionFavor();

        assertEquals(2, favor.resolveBonus(ModifierType.DAMAGE_REDUCTION, characterWithStrengthBase(3)));
        assertEquals(0, favor.resolveBonus(ModifierType.DAMAGE_REDUCTION, characterWithStrengthBase(2)));
    }

    @Test
    void resolvesNothingForAModifierTypeItDoesNotGrant() {
        ItemFavor favor = damageReductionFavor();

        assertEquals(0, favor.resolveBonus(ModifierType.ABSOLUTE_DAMAGE_REDUCTION, characterWithStrengthBase(3)));
    }

    @Test
    void sumsEveryBonusSharingTheSameModifierType() {
        ItemFavor favor = ItemFavor.builder()
                .bonus(new ItemBonus(ModifierType.DAMAGE_REDUCTION, 2))
                .bonus(new ItemBonus(ModifierType.DAMAGE_REDUCTION, 1))
                .bonus(new ItemBonus(ModifierType.DEFESAS, 1))
                .build();

        assertEquals(3, favor.resolveBonus(ModifierType.DAMAGE_REDUCTION, characterWithStrengthBase(1)));
        assertEquals(1, favor.resolveBonus(ModifierType.DEFESAS, characterWithStrengthBase(1)));
    }

    @Test
    void resolvesItsWholeBonusListOnlyWhenGranted() {
        ItemFavor favor = damageReductionFavor();

        assertEquals(1, favor.resolveBonuses(characterWithStrengthBase(3)).size());
        assertEquals(List.of(), favor.resolveBonuses(characterWithStrengthBase(2)));
    }

    private static ItemFavor damageReductionFavor() {
        return ItemFavor.builder()
                .description("Favor")
                .requirements(FORCA_3)
                .bonus(new ItemBonus(ModifierType.DAMAGE_REDUCTION, 2))
                .build();
    }

    private static Character characterWithStrengthBase(final int base) {
        return CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.builder()
                        .strength(AttributeValue.builder().domain(AttributeDomain.STRENGTH).base(base).build())
                        .build())
                .build();
    }
}
