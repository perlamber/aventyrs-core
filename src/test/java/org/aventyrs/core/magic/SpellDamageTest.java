package org.aventyrs.core.magic;

import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SpellDamageTest {

    @Test
    void halfFocusElementalIsZeroDiceZeroFlatHalfFocusOfThatElement() {
        SpellDamage damage = SpellDamage.halfFocusElemental(0, ElementalType.MAGMA);

        assertEquals(0, damage.diceCount());
        assertEquals(0, damage.flatBonus());
        assertEquals(FocusScaling.HALF, damage.focusScaling());
        assertEquals(DamageType.ELEMENTAL, damage.damageType());
        assertEquals(ElementalType.MAGMA, damage.elementalType());
    }

    @Test
    void halfFocusMagicalCarriesNoElement() {
        assertNull(SpellDamage.halfFocusMagical(2).elementalType());
    }

    @Test
    void anElementalTypeWithoutAnElementIsRejected() {
        assertThrows(IllegalOperationException.class,
                () -> new SpellDamage(1, 0, FocusScaling.HALF, DamageType.ELEMENTAL, null));
    }

    @Test
    void aNonElementalTypeWithAnElementIsRejected() {
        assertThrows(IllegalOperationException.class,
                () -> new SpellDamage(1, 0, FocusScaling.HALF, DamageType.MAGICO, ElementalType.FOGO));
    }

    @Test
    void aNegativeDiceCountIsRejected() {
        assertThrows(IllegalOperationException.class,
                () -> new SpellDamage(-1, 0, FocusScaling.NONE, DamageType.MAGICO, null));
    }

    @Test
    void aNullFocusScalingIsRejected() {
        assertThrows(IllegalOperationException.class,
                () -> new SpellDamage(1, 0, null, DamageType.MAGICO, null));
    }
}
