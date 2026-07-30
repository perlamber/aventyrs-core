package org.aventyrs.core.sheet;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterStatus;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class InteractionResultTest {

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    @Test
    void builderAssignsTheNextInteractableAndResultStatus() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();
        CharacterSheet next = CharacterSheet.of(character, new Player());

        InteractionResult result = InteractionResult.builder()
                .nextInteractable(next)
                .resultStatus(CharacterStatus.HIGH_LIFE)
                .build();

        assertSame(next, result.getNextInteractable());
        assertEquals(CharacterStatus.HIGH_LIFE, result.getResultStatus());
    }
}
