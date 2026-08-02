package org.aventyrs.core.character;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class CharacterTemplateTest {

    @Test
    void canBeInstantiated() {
        assertNotNull(new CharacterTemplate());
    }
}
