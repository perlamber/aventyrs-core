package org.aventyrs.core.item;

import org.aventyrs.core.character.DamageBase;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbstractWeaponTest {

    @Test
    void aWeaponCarriesBothItsOwnColumnsAndEveryInheritedOne() {
        AbstractWeapon machado = AbstractWeapon.builder()
                .name("Machado de Guerra")
                .category(ItemCategory.HEAVY_BLADE)
                .rarity(ItemRarity.COMMON)
                .weightClass(ItemWeightClass.HEAVY)
                .price(8)
                .hardness(20)
                .damageBase(DamageBase.of(2, 1))
                .skillType(SkillType.ATAQUE_CORPO_A_CORPO)
                .build();

        assertEquals(DamageBase.of(2, 1), machado.getDamageBase());
        assertEquals(SkillType.ATAQUE_CORPO_A_CORPO, machado.getSkillType());
        assertEquals("Machado de Guerra", machado.getName());
        assertEquals(ItemType.OFFENSIVE, machado.getType());
        assertEquals(20, machado.getHardness());
        assertNull(machado.getFavor());
    }

    /** A weapon is an Item everywhere an Item is expected — nothing about the split is a fork. */
    @Test
    void aWeaponIsAnItem() {
        Item item = AbstractWeapon.builder()
                .name("Adaga")
                .category(ItemCategory.LIGHT_BLADE)
                .damageBase(DamageBase.UNARMED)
                .skillType(SkillType.ATAQUE_CORPO_A_CORPO)
                .build();

        assertTrue(item instanceof Weapon);
        assertEquals(0, item.getPhysicalDefenseBonus());
    }

    /**
     * The two fields this builder does gatekeep: a null Dano Base is not a questionable value,
     * it's an NPE waiting for whoever asks what the weapon hits for — and a null Perícia is one
     * waiting for whoever asks which grants apply to the swing.
     */
    @Test
    void aWeaponMustNameItsDamageBase() {
        assertThrows(NullPointerException.class, () -> AbstractWeapon.builder()
                .name("Espada Sem Fio")
                .category(ItemCategory.LIGHT_BLADE)
                .skillType(SkillType.ATAQUE_CORPO_A_CORPO)
                .build());
    }

    @Test
    void aWeaponMustNameThePericiaItIsSwungWith() {
        assertThrows(NullPointerException.class, () -> AbstractWeapon.builder()
                .name("Espada Sem Dono")
                .category(ItemCategory.LIGHT_BLADE)
                .damageBase(DamageBase.of(1, 2))
                .build());
    }

    /** A weapon that never states an Alcance is a corpo-a-corpo one — Adjacente, not null. */
    @Test
    void aWeaponsRangeDefaultsToAdjacente() {
        AbstractWeapon dagger = AbstractWeapon.builder()
                .name("Adaga")
                .category(ItemCategory.LIGHT_BLADE)
                .damageBase(DamageBase.of(1, 2))
                .skillType(SkillType.ATAQUE_CORPO_A_CORPO)
                .build();

        assertEquals(Range.ADJACENTE, dagger.getRange());
        assertEquals(Range.ADJACENTE, dagger.getEffectiveRange());
    }

    @Test
    void aRangedWeaponAuthorsItsOwnAlcance() {
        AbstractWeapon longbow = AbstractWeapon.builder()
                .name("Arco Longo")
                .category(ItemCategory.BOW)
                .damageBase(DamageBase.of(1, 3))
                .skillType(SkillType.ATAQUE_A_DISTANCIA)
                .range(Range.DISTANCIA_LONGA)
                .build();

        assertEquals(Range.DISTANCIA_LONGA, longbow.getRange());
        assertEquals(Range.DISTANCIA_LONGA, longbow.getEffectiveRange());
    }

    /** Same shape as getEffectiveDamageBase dropping to UNARMED — a wreck reaches no further than a fist. */
    @Test
    void aDestroyedWeaponReachesOnlyAdjacenteWhileKeepingItsAuthoredColumn() {
        AbstractWeapon longbow = AbstractWeapon.builder()
                .name("Arco Longo")
                .category(ItemCategory.BOW)
                .hardness(6)
                .damageBase(DamageBase.of(1, 3))
                .skillType(SkillType.ATAQUE_A_DISTANCIA)
                .range(Range.DISTANCIA_LONGA)
                .build();

        longbow.applyDamage(6);

        assertEquals(Range.ADJACENTE, longbow.getEffectiveRange());
        assertEquals(Range.DISTANCIA_LONGA, longbow.getRange());
    }
}
