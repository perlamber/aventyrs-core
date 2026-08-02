package org.aventyrs.core.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class RollErrorExceptionTest {

    @Test
    void canBeThrownAndCaughtAsARuntimeException() {
        assertThrows(RollErrorException.class, () -> {
            throw new RollErrorException();
        });
    }
}
