package org.aventyrs.core.skill.attention;

import org.aventyrs.core.character.services.CharacterSkillService;
import org.aventyrs.core.modifier.ModifierResolver;
import org.aventyrs.core.skill.AbstractSkillInteraction;
import org.aventyrs.core.skill.SkillType;

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
