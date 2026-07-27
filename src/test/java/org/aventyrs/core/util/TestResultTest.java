package org.aventyrs.core.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TestResultTest {

    @Test
    void hasTheFourExpectedOutcomes() {
        assertEquals(4, TestResult.values().length);
        assertNotNull(TestResult.valueOf("PASSED"));
        assertNotNull(TestResult.valueOf("FAILED"));
        assertNotNull(TestResult.valueOf("HIGH_BELLOW"));
        assertNotNull(TestResult.valueOf("HIGH_ABOVE"));
    }
}
