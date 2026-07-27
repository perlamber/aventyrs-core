package org.aventyrs.core.sheet;

import org.aventyrs.core.character.CharacterStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class InteractionResultTest {

    @Test
    void builderAssignsTheNextInteractableAndResultStatus() {
        Interactable next = new Interactable() {
            @Override
            public CharacterStatus receiveInteraction(Interaction interaction) {
                return CharacterStatus.CLEAN;
            }

            @Override
            public CharacterStatus receiveInteraction() {
                return CharacterStatus.CLEAN;
            }
        };
        InteractionResult result = InteractionResult.builder()
                .nextInteractable(next)
                .resultStatus(CharacterStatus.HIGH_LIFE)
                .build();

        assertSame(next, result.getNextInteractable());
        assertEquals(CharacterStatus.HIGH_LIFE, result.getResultStatus());
    }
}
