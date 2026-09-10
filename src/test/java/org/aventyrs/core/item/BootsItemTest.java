package org.aventyrs.core.item;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.skill.Skill;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BootsItemTest {

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    @Test
    void hasOneConstantPerCatalogedBoot() {
        assertEquals(6, BootsItem.values().length);
    }

    @Test
    void everyConstantIsANamedDefensiveBoot() {
        // Botas are not CriticallyDefensiveItem — that BootsItem cannot be cast to it is a
        // compile-time guarantee, so there is nothing to assert at runtime.
        Arrays.stream(BootsItem.values()).forEach(boot -> {
            assertEquals(ItemCategory.BOOTS, boot.getCategory());
            assertEquals(ItemType.DEFENSIVE, boot.getType());
            assertFalse(boot.getName().isBlank());
        });
    }

    @Test
    void everyCatalogedFavorIsDescribedAndCarriesItsRequirements() {
        Arrays.stream(BootsItem.values())
                .map(BootsItem::getFavor)
                .filter(Objects::nonNull)
                .forEach(favor -> {
                    assertFalse(favor.getDescription().isBlank());
                    assertNotNull(favor.getRequirements());
                });
    }

    @Test
    void botasDoAventureiroCarriesEveryColumnOfItsRulesText() {
        BootsItem boot = BootsItem.BOTAS_DO_AVENTUREIRO;

        assertEquals("Botas do Aventureiro", boot.getName());
        assertEquals(ItemWeightClass.LIGHT, boot.getWeightClass());
        assertEquals(ItemRarity.COMMON, boot.getRarity());
        assertEquals(2, boot.getPrice());
        assertEquals(0, boot.getPhysicalDefenseBonus());
        assertEquals(0, boot.getMagicDefenseBonus());
        assertEquals(20, boot.getHardness());
        assertEquals(0, boot.getCastingBonus());
    }

    @Test
    void botasDoAventureiroGrantsAFlatMovementBonusOnlyAtDestreza3() {
        assertEquals(1, BootsItem.BOTAS_DO_AVENTUREIRO
                .resolveFavorBonus(ModifierType.MOVEMENT, characterWithBase(AttributeDomain.DEXTERITY, 3)));
        assertEquals(0, BootsItem.BOTAS_DO_AVENTUREIRO
                .resolveFavorBonus(ModifierType.MOVEMENT, characterWithBase(AttributeDomain.DEXTERITY, 2)));
    }

    @Test
    void sandalhasDoCorredorGrantsAFlatMovementBonusForItsGroundEfeitoAdicional() {
        assertEquals(2, BootsItem.SANDALHAS_DO_CORREDOR
                .resolveFavorBonus(ModifierType.MOVEMENT, characterWithBase(AttributeDomain.DEXTERITY, 3)));
    }

    @Test
    void sandalhasDosPequeninosGrantsBothMovementAndAFurtividadeVantagem() {
        Character character = characterWithBase(AttributeDomain.DEXTERITY, 3);

        assertEquals(1, BootsItem.SANDALHAS_DOS_PEQUENINOS
                .resolveFavorBonus(ModifierType.MOVEMENT, character));
        assertEquals(Skill.ADVANTAGE_BONUS, BootsItem.SANDALHAS_DOS_PEQUENINOS
                .resolveFavorBonus(ModifierType.FURTIVIDADE_ROLL_BONUS, character));
    }

    /** The vertical/climb axis, the swim axis and the Reposicionar manoeuvre are all unmodelled. */
    @Test
    void grantsNoBonusForItsUnmodelledMovementAxisClauses() {
        Character strong = characterWithBase(AttributeDomain.STRENGTH, 3);

        assertEquals(0, BootsItem.BOTINAS_DE_ESCALADA.resolveFavorBonus(ModifierType.MOVEMENT, strong));
        assertEquals(0, BootsItem.NADADEIRAS_DECIEMBRANAS.resolveFavorBonus(ModifierType.MOVEMENT, strong));
        assertTrue(BootsItem.BOTINAS_DE_ESCALADA.getFavor().getBonuses().isEmpty());
        assertTrue(BootsItem.NADADEIRAS_DECIEMBRANAS.getFavor().getBonuses().isEmpty());
    }

    /** The DF/DM columns are real even though the "enquanto em movimento" Favor is not. */
    @Test
    void grevasDosAnoesCarriesRealDefenseColumnsButNoWhileMovingBonus() {
        assertEquals(1, BootsItem.GREVAS_DOS_ANOES.getPhysicalDefenseBonus());
        assertEquals(1, BootsItem.GREVAS_DOS_ANOES.getMagicDefenseBonus());
        assertTrue(BootsItem.GREVAS_DOS_ANOES.getFavor().getBonuses().isEmpty());
        assertTrue(BootsItem.GREVAS_DOS_ANOES.getFavor().hasAdditionalEffects());
    }

    private static Character characterWithBase(final AttributeDomain domain, final int base) {
        return CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.of(Map.of(domain, base)))
                .build();
    }
}
