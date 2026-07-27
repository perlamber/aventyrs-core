package org.aventyrs.core.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class NoValueExceptionTest {

    @Test
    void canBeThrownAndCaughtAsARuntimeException() {
        assertThrows(NoValueException.class, () -> {
            throw new NoValueException();
        });
    }
}
