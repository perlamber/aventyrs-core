package org.aventyrs.core.character;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DamageBonusTest {

    @Test
    void constructorAssignsValueAndType() {
        DamageBonus damageBonus = new DamageBonus(2, DamageType.FISICO);

        assertEquals(2, damageBonus.getValue());
        assertEquals(DamageType.FISICO, damageBonus.getType());
    }
}
