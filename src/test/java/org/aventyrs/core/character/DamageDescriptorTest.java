package org.aventyrs.core.character;

import org.aventyrs.core.magic.ElementalType;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DamageDescriptorTest {

    @Test
    void nonElementalDamageCarriesNoElement() {
        DamageDescriptor descriptor = new DamageDescriptor(DamageType.FISICO);

        assertEquals(DamageType.FISICO, descriptor.damageType());
        assertNull(descriptor.elementalType());
    }

    @Test
    void elementalDamageCarriesItsSpecificElement() {
        DamageDescriptor descriptor = new DamageDescriptor(DamageType.FISICO_ELEMENTAL, ElementalType.GELO);

        assertEquals(DamageType.FISICO_ELEMENTAL, descriptor.damageType());
        assertEquals(ElementalType.GELO, descriptor.elementalType());
    }

    @Test
    void elementalDamageRequiresAnElement() {
        assertThrows(IllegalOperationException.class, () -> new DamageDescriptor(DamageType.ELEMENTAL));
    }

    @Test
    void nonElementalDamageRejectsAnElement() {
        assertThrows(IllegalOperationException.class,
                () -> new DamageDescriptor(DamageType.MAGICO, ElementalType.FOGO));
    }

    @Test
    void incomingDamageRejectsAllElementsMarker() {
        assertThrows(IllegalOperationException.class,
                () -> new DamageDescriptor(DamageType.ELEMENTAL, ElementalType.TODOS));
    }
}
