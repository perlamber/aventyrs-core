package org.aventyrs.core.sheet;

import org.aventyrs.core.character.CharacterStatus;

public interface Interactable<R extends Interactable<R>> {
    public InteractionResult receiveInteraction(Interaction<R> interaction);
}
