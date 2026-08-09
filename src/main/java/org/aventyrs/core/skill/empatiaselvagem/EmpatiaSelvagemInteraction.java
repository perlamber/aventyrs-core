package org.aventyrs.core.skill.empatiaselvagem;

import org.aventyrs.core.character.services.CharacterSkillService;
import org.aventyrs.core.modifier.ModifierResolver;
import org.aventyrs.core.skill.AbstractSkillInteraction;
import org.aventyrs.core.skill.SkillType;

/**
 * Requests an Empatia Selvagem Perícia test. Which of Empatia Selvagem's specializations
 * ({@link EmpatiaSelvagemSpecialization}) the roll is for doesn't change {@code
 * skillRollBonus}/{@code difficultyReduction} — a held one can still be requested via
 * {@code SkillRoll#getRequestedAbility()}, switching {@code reachedDifficultyLevel} to expert
 * thresholds; see {@link AbstractSkillInteraction} for how that and the roll
 * bonus/difficultyReduction are actually computed.
 */
public class EmpatiaSelvagemInteraction extends AbstractSkillInteraction {

    public EmpatiaSelvagemInteraction() {
        super(SkillType.EMPATIA_SELVAGEM);
    }

    public EmpatiaSelvagemInteraction(final CharacterSkillService characterSkillService, final ModifierResolver modifierResolver) {
        super(SkillType.EMPATIA_SELVAGEM, characterSkillService, modifierResolver);
    }
}
