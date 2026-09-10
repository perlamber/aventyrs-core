package org.aventyrs.core.item;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.DamageBase;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.magic.TestSpell;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AttackMethodTest {

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static Weapon weapon(final ItemCategory category) {
        return AbstractWeapon.builder().name(category.name()).category(category)
                .damageBase(DamageBase.of(1, 1)).skillType(SkillType.ATAQUE_CORPO_A_CORPO).build();
    }

    private static Character character() {
        return CharacterFixture.blank(CharacterFixture.BLANK).build();
    }

    @Test
    void matchesTheWeaponOfTheSameCategoryOnly() {
        Weapon bow = weapon(ItemCategory.BOW);

        assertTrue(AttackMethod.BOW.matches(bow, character()));
        assertFalse(AttackMethod.CROSSBOW.matches(bow, character()));
    }

    @Test
    void naturalWeaponMatchesViaTreatsAsNaturalWeaponNotRawCategory() {
        Weapon naturalWeapon = weapon(ItemCategory.NATURAL_WEAPON);

        assertTrue(AttackMethod.NATURAL_WEAPON.matches(naturalWeapon, character()));
    }

    @Test
    void offensiveMagicMatchesOnlyASpell() {
        Weapon bow = weapon(ItemCategory.BOW);

        assertTrue(AttackMethod.OFFENSIVE_MAGIC.matches(new TestSpell(), character()));
        assertFalse(AttackMethod.OFFENSIVE_MAGIC.matches(bow, character()));
        assertFalse(AttackMethod.BOW.matches(new TestSpell(), character()));
    }

    @Test
    void nullAttackSourceNeverMatches() {
        assertFalse(AttackMethod.BOW.matches(null, character()));
        assertFalse(AttackMethod.OFFENSIVE_MAGIC.matches(null, character()));
    }
}
