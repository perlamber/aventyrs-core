package org.aventyrs.core.skill.conhecimentos;

import org.aventyrs.core.character.services.CharacterSkillService;
import org.aventyrs.core.modifier.ModifierResolver;
import org.aventyrs.core.skill.AbstractSkillInteraction;
import org.aventyrs.core.skill.SkillType;

/**
 * Requests a Conhecimentos Perícia test. Which of Conhecimentos' specializations ({@link
 * ConhecimentosSpecialization}) the roll is for doesn't change {@code skillRollBonus}/{@code
 * difficultyReduction} — a held one can still be requested via {@code
 * SkillRoll#getRequestedAbility()}, switching {@code reachedDifficultyLevel} to expert
 * thresholds; see {@link AbstractSkillInteraction} for how that and the roll
 * bonus/difficultyReduction are actually computed.
 */
public class ConhecimentosInteraction extends AbstractSkillInteraction {

    public ConhecimentosInteraction() {
        super(SkillType.CONHECIMENTOS);
    }

    public ConhecimentosInteraction(final CharacterSkillService characterSkillService, final ModifierResolver modifierResolver) {
        super(SkillType.CONHECIMENTOS, characterSkillService, modifierResolver);
    }
}
