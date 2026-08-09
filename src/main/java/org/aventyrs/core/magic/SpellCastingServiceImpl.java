package org.aventyrs.core.magic;

import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.Interaction;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.skill.dominiodomana.DominioDoManaInteraction;
import org.aventyrs.core.skill.dominiodomana.DominioDoManaInteraction;

public class SpellCastingServiceImpl implements SpellCastingService {

    private final Interaction<CharacterSheet> dominioDoManaInteraction;

    public SpellCastingServiceImpl() {
        this(new DominioDoManaInteraction());
    }

    public SpellCastingServiceImpl(final Interaction<CharacterSheet> dominioDoManaInteraction) {
        this.dominioDoManaInteraction = dominioDoManaInteraction;
    }

    @Override
    public SpellCastingResult castSpell(final CharacterSheet target, final Interaction<CharacterSheet> deliveryInteraction) {
        InteractionResult deliveryResult = target.receiveInteraction(deliveryInteraction);
        InteractionResult dominioDoManaResult = target.receiveInteraction(dominioDoManaInteraction);
        return SpellCastingResult.builder()
                .deliveryResult(deliveryResult)
                .dominioDoManaResult(dominioDoManaResult)
                .build();
    }
}
