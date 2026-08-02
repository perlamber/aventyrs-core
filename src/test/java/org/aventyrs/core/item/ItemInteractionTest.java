package org.aventyrs.core.item;

import org.aventyrs.core.sheet.Interaction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemInteractionTest {

    @Test
    void isAnInteraction() {
        assertTrue(new ItemInteraction() instanceof Interaction);
    }
}
