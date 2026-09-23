package org.aventyrs.core.character;

import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.item.AbstractItem;
import org.aventyrs.core.item.AbstractWeapon;
import org.aventyrs.core.item.ItemCategory;
import org.aventyrs.core.item.Weapon;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** {@link Character#isArmedOnlyWithNaturalWeapons()} and {@link Character#usesDefensiveEquipment()}. */
class CharacterNaturalWeaponsOnlyTest {

    private Character character;

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        character = CharacterFixture.blank(CharacterFixture.BLANK)
                .feats(new ArrayList<>())
                .equipment(new ArrayList<>())
                .drawnWeapons(new ArrayList<>())
                .build();
    }

    @Test
    void nothingDrawnIsArmedOnlyWithNaturalWeapons() {
        assertTrue(character.isArmedOnlyWithNaturalWeapons());
    }

    @Test
    void aDrawnBladeIsNotButASheathedOneIs() {
        Weapon sword = AbstractWeapon.builder().name("Espada").category(ItemCategory.HEAVY_BLADE)
                .damageBase(DamageBase.of(2, 0)).skillType(SkillType.ATAQUE_CORPO_A_CORPO).build();
        character.equip(sword);
        assertTrue(character.isArmedOnlyWithNaturalWeapons());

        character.drawWeapon(sword);
        assertFalse(character.isArmedOnlyWithNaturalWeapons());
    }

    @Test
    void anyDefensiveItemCountsAsEquipamentoDefensivo() {
        assertFalse(character.usesDefensiveEquipment());

        character.equip(AbstractItem.builder().name("Anel").category(ItemCategory.RING).build());
        assertFalse(character.usesDefensiveEquipment());

        character.equip(AbstractItem.builder().name("Elmo").category(ItemCategory.HELMET).build());
        assertTrue(character.usesDefensiveEquipment());
    }
}
