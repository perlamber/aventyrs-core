package org.aventyrs.core.skill.profissao;

import org.aventyrs.core.character.services.CharacterSkillService;
import org.aventyrs.core.modifier.ModifierResolver;
import org.aventyrs.core.skill.AbstractSkillInteraction;
import org.aventyrs.core.skill.SkillType;

/**
 * Requests a Profissão Perícia test. Which of Profissão's specializations ({@link
 * ProfissaoSpecialization}) the roll is for doesn't change {@code skillRollBonus}/{@code
 * difficultyReduction} — a held one can still be requested via {@code
 * SkillRoll#getRequestedAbility()}, switching {@code reachedDifficultyLevel} to expert
 * thresholds; see {@link AbstractSkillInteraction} for how that and the roll
 * bonus/difficultyReduction are actually computed.
 */
public class ProfissaoInteraction extends AbstractSkillInteraction {

    public ProfissaoInteraction() {
        super(SkillType.PROFISSAO);
    }

    public ProfissaoInteraction(final CharacterSkillService characterSkillService, final ModifierResolver modifierResolver) {
        super(SkillType.PROFISSAO, characterSkillService, modifierResolver);
    }
}
