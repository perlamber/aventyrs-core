package org.aventyrs.core.character;

import org.aventyrs.core.sheet.IllegalOperationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DamageBonusTest {

    @Test
    void constructorAssignsValueAndType() {
        DamageBonus damageBonus = new DamageBonus(2, DamageType.FISICO);

        assertEquals(2, damageBonus.getValue());
        assertEquals(DamageType.FISICO, damageBonus.getType());
        assertNull(damageBonus.getElement());
    }

    @Test
    void constructorAssignsValueTypeAndElementForAnElementalType() {
        DamageBonus damageBonus = new DamageBonus(2, DamageType.ELEMENTAL, Element.TERRA);

        assertEquals(2, damageBonus.getValue());
        assertEquals(DamageType.ELEMENTAL, damageBonus.getType());
        assertEquals(Element.TERRA, damageBonus.getElement());
    }

    @Test
    void constructorRejectsAnElementalTypeWithNoElement() {
        assertThrows(IllegalOperationException.class,
                () -> new DamageBonus(2, DamageType.FISICO_ELEMENTAL));
    }

    @Test
    void constructorRejectsANonElementalTypeWithAnElement() {
        assertThrows(IllegalOperationException.class,
                () -> new DamageBonus(2, DamageType.PRIMORDIAL, Element.FOGO));
    }
}
