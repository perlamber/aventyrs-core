package org.aventyrs.core.skill;

import org.aventyrs.core.character.services.CharacterSkillService;
import org.aventyrs.core.modifier.ModifierResolver;

/**
 * Requests a Profissão Perícia test. Which of Profissão's specializations the roll is for
 * doesn't change the bonus — see {@link ProfissaoSpecialization} — so it isn't tracked here.
 * See {@link AbstractSkillInteraction} for how the roll bonus/difficultyReduction are
 * actually computed.
 */
public class ProfissaoInteraction extends AbstractSkillInteraction {

    public ProfissaoInteraction() {
        super(SkillType.PROFISSAO);
    }

    public ProfissaoInteraction(final CharacterSkillService characterSkillService, final ModifierResolver modifierResolver) {
        super(SkillType.PROFISSAO, characterSkillService, modifierResolver);
    }
}
