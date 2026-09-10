package org.aventyrs.core.skill;

import org.aventyrs.core.character.DamageBase;
import org.aventyrs.core.item.AbstractItem;
import org.aventyrs.core.item.AbstractWeapon;
import org.aventyrs.core.item.Item;
import org.aventyrs.core.item.ItemCategory;
import org.aventyrs.core.item.Weapon;
import org.aventyrs.core.magic.Spell;
import org.aventyrs.core.magic.TestSpell;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link AttackSource} has no behaviour of its own to test — what's worth pinning is that the two
 * things an attack can be made with really are one, that a weapon's two Perícia columns can't
 * drift apart, and that a non-weapon Item still isn't one.
 */
class AttackSourceTest {

    private static final Weapon MACHADO = AbstractWeapon.builder()
            .name("Machado de Batalha")
            .category(ItemCategory.HEAVY_BLADE)
            .damageBase(DamageBase.of(2, 1))
            .skillType(SkillType.ATAQUE_CORPO_A_CORPO)
            .build();

    @Test
    void aWeaponIsAnAttackSource() {
        assertInstanceOf(AttackSource.class, MACHADO);
    }

    @Test
    void aSpellIsAnAttackSource() {
        assertInstanceOf(AttackSource.class, new TestSpell());
    }

    /**
     * The default delegates to {@code getSkillType()} rather than being separately authorable,
     * so a weapon can't present one Perícia to {@code DamageBaseService} and another to a roll.
     */
    @Test
    void aWeaponsAttackSkillTypeIsItsOwnSkillType() {
        AttackSource attackSource = MACHADO;

        assertEquals(MACHADO.getSkillType(), attackSource.getAttackSkillType());
        assertEquals(SkillType.ATAQUE_CORPO_A_CORPO, attackSource.getAttackSkillType());
    }

    @Test
    void aSpellReportsWhicheverPericiaDeliversIt() {
        Spell toque = new TestSpell(SkillType.ATAQUE_CORPO_A_CORPO);

        assertEquals(SkillType.ATAQUE_CORPO_A_CORPO, toque.getAttackSkillType());
        assertEquals(SkillType.ATAQUE_A_DISTANCIA, new TestSpell().getAttackSkillType());
    }

    /**
     * Only {@code Weapon} extends {@link AttackSource}, not {@code Item} — the same
     * enforcement-by-type discipline that keeps {@code getDamageBase()} off a pauldron. A hook
     * narrowing with {@code instanceof Weapon} therefore can't be handed a helmet.
     */
    @Test
    void aNonWeaponItemIsNotAnAttackSource() {
        Item elmo = AbstractItem.builder()
                .name("Elmo")
                .category(ItemCategory.HELMET)
                .build();

        assertFalse(elmo instanceof AttackSource);
        assertTrue(MACHADO instanceof AttackSource);
    }
}
