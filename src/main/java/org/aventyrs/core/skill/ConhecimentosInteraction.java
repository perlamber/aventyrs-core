package org.aventyrs.core.skill;

import org.aventyrs.core.character.services.CharacterSkillService;
import org.aventyrs.core.modifier.ModifierResolver;

/**
 * Requests a Conhecimentos Perícia test. Which of Conhecimentos' specializations the roll is
 * for doesn't change the bonus — see {@link ConhecimentosSpecialization} — so it isn't tracked
 * here. See {@link AbstractSkillInteraction} for how the roll bonus/difficultyReduction are
 * actually computed.
 */
public class ConhecimentosInteraction extends AbstractSkillInteraction {

    public ConhecimentosInteraction() {
        super(SkillType.CONHECIMENTOS);
    }

    public ConhecimentosInteraction(final CharacterSkillService characterSkillService, final ModifierResolver modifierResolver) {
        super(SkillType.CONHECIMENTOS, characterSkillService, modifierResolver);
    }
}
