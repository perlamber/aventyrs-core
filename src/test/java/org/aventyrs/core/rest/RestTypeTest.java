package org.aventyrs.core.rest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RestTypeTest {

    @Test
    void everyRestTypeIsAtLeastItself() {
        for (RestType restType : RestType.values()) {
            assertTrue(restType.isAtLeast(restType));
        }
    }

    @Test
    void strongerRestTypesAreAtLeastWeakerOnes() {
        assertTrue(RestType.LONGO.isAtLeast(RestType.CURTO));
        assertTrue(RestType.TOTAL.isAtLeast(RestType.LONGO));
    }

    @Test
    void weakerRestTypesAreNotAtLeastStrongerOnes() {
        assertFalse(RestType.CURTO.isAtLeast(RestType.LONGO));
        assertFalse(RestType.MINIMO.isAtLeast(RestType.CURTO));
    }
}
