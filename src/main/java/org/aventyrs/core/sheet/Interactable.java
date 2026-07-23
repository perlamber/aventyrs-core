package org.aventyrs.core.sheet;

import org.aventyrs.core.character.CharacterStatus;

public interface Interactable {
    public CharacterStatus receiveInteraction(Interaction interaction);
    public CharacterStatus receiveInteraction();
}
