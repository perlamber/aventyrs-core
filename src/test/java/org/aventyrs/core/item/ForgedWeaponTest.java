package org.aventyrs.core.item;

import org.aventyrs.core.sheet.IllegalOperationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A forged copy of a catalog Arma must still <em>be</em> an Arma. Built as a bare {@link
 * AbstractItem} it could not be drawn, named as an attack's {@code AttackSource} or handed to
 * {@code DamageBaseService} — a bought sword nobody could swing.
 */
class ForgedWeaponTest {

    private static final LightBladeItem BLADE = LightBladeItem.values()[0];
    private static final ArmorItem ARMOUR = ArmorItem.values()[0];

    @Test
    void aForgedCopyOfAWeaponTemplateIsAWeaponWithTheTemplatesOwnColumns() throws IllegalOperationException {
        Item forged = ItemForgery.purchased(ItemSpecification.builder().base(BLADE).build()).forge();

        Weapon weapon = assertInstanceOf(Weapon.class, forged, "a forged Arma must be a Weapon");
        assertEquals(BLADE.getDamageBase(), weapon.getDamageBase());
        assertEquals(BLADE.getSkillType(), weapon.getSkillType());
        assertEquals(BLADE.getRange(), weapon.getRange());
        assertEquals(BLADE.getCategory(), weapon.getCategory());
        assertEquals(BLADE.getWeightClass(), weapon.getWeightClass());
    }

    /** The copy is drawable, which is what the whole distinction is for. */
    @Test
    void aForgedWeaponCanBeCarriedAndDrawn() throws IllegalOperationException {
        Item forged = ItemForgery.purchased(ItemSpecification.builder().base(BLADE).build()).forge();
        org.aventyrs.core.character.Character character =
                org.aventyrs.core.character.fixture.CharacterFixture.blank(
                        org.aventyrs.core.character.fixture.CharacterFixture.BLANK)
                        .equipment(new java.util.ArrayList<>())
                        .drawnWeapons(new java.util.ArrayList<>())
                        .build();

        character.equip(forged);

        assertTrue(character.drawWeapon((Weapon) forged));
        assertEquals(java.util.List.of(forged), character.getDrawnWeapons());
    }

    @Test
    void aForgedCopyOfANonWeaponTemplateIsStillAPlainItem() throws IllegalOperationException {
        Item forged = ItemForgery.purchased(ItemSpecification.builder().base(ARMOUR).build()).forge();

        assertFalse(forged instanceof Weapon, "armour is not a weapon");
        assertEquals(ARMOUR.getPhysicalDefenseBonus(), forged.getPhysicalDefenseBonus());
    }

    @Test
    void fromTemplateKeepsTheSameDistinction() {
        assertInstanceOf(Weapon.class, AbstractItem.fromTemplate(BLADE));
        assertFalse(AbstractItem.fromTemplate(ARMOUR) instanceof Weapon);
    }
}
