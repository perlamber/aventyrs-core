package org.aventyrs.core.skill.furtividade;

import org.aventyrs.core.character.services.CharacterSkillService;
import org.aventyrs.core.modifier.ModifierResolver;
import org.aventyrs.core.skill.AbstractSkillInteraction;
import org.aventyrs.core.skill.SkillType;

/**
 * Requests a Furtividade Perícia test. Which of Furtividade's specializations the roll is for
 * doesn't change the bonus — see {@link FurtividadeSpecialization} — so it isn't tracked here.
 * The rules text also has an opposed Atenção roll identify a hidden character; that resolution
 * is left to a layer above this core, same as every other cross-character contest. See {@link
 * AbstractSkillInteraction} for how the roll bonus/difficultyReduction are actually computed.
 */
public class FurtividadeInteraction extends AbstractSkillInteraction {

    public FurtividadeInteraction() {
        super(SkillType.FURTIVIDADE);
    }

    public FurtividadeInteraction(final CharacterSkillService characterSkillService, final ModifierResolver modifierResolver) {
        super(SkillType.FURTIVIDADE, characterSkillService, modifierResolver);
    }
}
