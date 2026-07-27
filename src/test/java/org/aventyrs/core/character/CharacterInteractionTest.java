package org.aventyrs.core.character;

import org.aventyrs.core.sheet.Interaction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CharacterInteractionTest {

    @Test
    void builderProducesAnInteraction() {
        CharacterInteraction interaction = CharacterInteraction.builder().build();
        assertNotNull(interaction);
        assertTrue(interaction instanceof Interaction);
    }

    @Test
    void toBuilderProducesAnEquivalentInstance() {
        CharacterInteraction interaction = CharacterInteraction.builder().build();
        assertNotNull(interaction.toBuilder().build());
    }
}
