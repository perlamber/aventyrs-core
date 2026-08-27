package org.aventyrs.core.character;

import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.Interactable;
import org.aventyrs.core.sheet.Interaction;
import org.aventyrs.core.sheet.InteractionResult;

import lombok.Builder;

@Builder(toBuilder = true)
public class CharacterInteraction implements Interaction<CombatantSheet> {
    //TODO implement
    @Override
    public InteractionResult applyTo(CombatantSheet target) {
        return null;
    }
}
