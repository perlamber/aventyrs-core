package org.aventyrs.core.sheet;

import org.aventyrs.core.character.CharacterStatus;

import lombok.Builder;
import lombok.Getter;

/**
 *
 */
@Getter @Builder
public class InteractionResult {
    Interactable nextInteractable;
    CharacterStatus resultStatus;

}
