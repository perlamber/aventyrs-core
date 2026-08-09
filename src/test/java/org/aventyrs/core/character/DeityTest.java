package org.aventyrs.core.character;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class DeityTest {

    @Test
    void everyDeityHasADescription() {
        for (Deity deity : Deity.values()) {
            assertFalse(deity.getDescription().isBlank());
        }
    }

    @Test
    void listHasTheTwentyEightDescribedDeities() {
        assertEquals(28, Deity.values().length);
    }

    @Test
    void nenhumaBelongsToItsOwnCategory() {
        assertEquals(DeityCategory.NENHUMA, Deity.NENHUMA.getCategory());
    }

    @Test
    void everyOtherDeityBelongsToADeityCategory() {
        for (Deity deity : Deity.values()) {
            if (deity == Deity.NENHUMA) {
                continue;
            }
            assertFalse(deity.getCategory() == DeityCategory.NENHUMA);
        }
    }
}
