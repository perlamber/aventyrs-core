package org.aventyrs.core.character;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AttributeValueTest {

    @Test
    void defaultsToBaseOneWithNoBonuses() {
        AttributeValue value = AttributeValue.builder().build();
        assertEquals(1, value.getTotal());
    }

    @Test
    void totalSumsAllThreeComponents() {
        AttributeValue value = AttributeValue.builder().base(3).racialBonus(1).variable(2).build();
        assertEquals(6, value.getTotal());
    }
}
