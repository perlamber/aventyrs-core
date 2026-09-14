package org.aventyrs.core.skill.attention;

import org.aventyrs.core.character.services.CharacterSkillService;
import org.aventyrs.core.modifier.ModifierResolver;
import org.aventyrs.core.skill.AbstractSkillInteraction;
import org.aventyrs.core.skill.SkillType;

/**
 * Requests an Attention Perícia test. See {@link AbstractSkillInteraction} for how the roll
 * bonus/difficultyReduction are actually computed.
 *
 * <p>This is the roll that sees through a hidden character: its total is what {@code
 * org.aventyrs.core.character.services.HidingService#resolveDetection} opposes to the hider's
 * Furtividade. A {@code org.aventyrs.core.monster.MonsterSheet} never makes it — a foe presents an
 * authored flat Atenção instead, the same way it presents its Defesas.
 */
public class AttentionInteraction extends AbstractSkillInteraction {

    public AttentionInteraction() {
        super(SkillType.ATTENTION);
    }

    public AttentionInteraction(final CharacterSkillService characterSkillService, final ModifierResolver modifierResolver) {
        super(SkillType.ATTENTION, characterSkillService, modifierResolver);
    }
}
