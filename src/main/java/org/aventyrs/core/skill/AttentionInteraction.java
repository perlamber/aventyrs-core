package org.aventyrs.core.skill;

import org.aventyrs.core.character.services.CharacterSkillService;
import org.aventyrs.core.modifier.ModifierResolver;

/**
 * Requests an Attention Perícia test. See {@link AbstractSkillInteraction} for how the roll
 * bonus/difficultyReduction are actually computed.
 */
public class AttentionInteraction extends AbstractSkillInteraction {

    public AttentionInteraction() {
        super(SkillType.ATTENTION);
    }

    public AttentionInteraction(final CharacterSkillService characterSkillService, final ModifierResolver modifierResolver) {
        super(SkillType.ATTENTION, characterSkillService, modifierResolver);
    }
}
