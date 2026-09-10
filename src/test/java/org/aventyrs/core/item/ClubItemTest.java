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
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClubItemTest {

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    @Test
    void hasOneConstantPerCatalogedClava() {
        assertEquals(5, ClubItem.values().length);
    }

    @Test
    void everyConstantIsANamedAdjacentMeleeOffensiveWeapon() {
        Arrays.stream(ClubItem.values()).forEach(club -> {
            assertEquals(ItemCategory.CLUB, club.getCategory());
            assertEquals(ItemType.OFFENSIVE, club.getType());
            assertFalse(club.getName().isBlank());
            assertEquals(SkillType.ATAQUE_CORPO_A_CORPO, club.getSkillType());
            assertEquals(Range.ADJACENTE, club.getRange());
            assertEquals(0, club.getHardness());
            assertEquals(0, club.getPhysicalDefenseBonus());
            assertEquals(0, club.getCastingBonus());
        });
    }

    @Test
    void bordaoCarriesEveryColumnOfItsRulesText() {
        ClubItem club = ClubItem.BORDAO_OU_BO;

        assertEquals("Bordão ou Bo", club.getName());
        assertEquals(ItemWeightClass.MEDIUM, club.getWeightClass());
        assertEquals(ItemRarity.COMMON, club.getRarity());
        assertEquals(7, club.getPrice());
        assertEquals(DamageBase.of(1, 2), club.getDamageBase());
        assertEquals(CriticalEffectType.ATORDOANTE, club.getCriticalEffect());
        assertEquals(17, club.getLesserCriticalMargin());
    }

    @Test
    void marteloCarriesEstilhacador() {
        assertEquals(CriticalEffectType.ESTILHACADOR, ClubItem.MARTELO_DE_GUERRA_OU_TETSUBO.getCriticalEffect());
        assertEquals(DamageBase.of(1, 3), ClubItem.MARTELO_DE_GUERRA_OU_TETSUBO.getDamageBase());
        assertEquals(ItemRarity.RARE, ClubItem.MARTELO_DE_GUERRA_OU_TETSUBO.getRarity());
    }

    @Test
    void bordaoFavorGrantsAFlatDefesasBonusOnlyAtDestreza3() {
        ClubItem club = ClubItem.BORDAO_OU_BO;

        assertEquals(2, club.resolveFavorBonus(ModifierType.DEFESAS, characterWithDexterity(3)));
        assertEquals(0, club.resolveFavorBonus(ModifierType.DEFESAS, characterWithDexterity(2)));
    }

    /** The Danos-Críticos and "muda para" Favores are prose — crit dano is not modeled. */
    @Test
    void everyOtherClubFavorGrantsNoRealBonus() {
        Arrays.stream(ClubItem.values())
                .filter(club -> club != ClubItem.BORDAO_OU_BO)
                .forEach(club -> assertTrue(club.getFavor().getBonuses().isEmpty(), club.getName()));
    }

    private static Character characterWithDexterity(final int base) {
        AttributeValue value = AttributeValue.builder().domain(AttributeDomain.DEXTERITY).base(base).build();
        return CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.builder().dexterity(value).build())
                .build();
    }
}
