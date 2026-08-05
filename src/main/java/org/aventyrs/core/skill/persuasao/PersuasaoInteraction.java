package org.aventyrs.core.skill.persuasao;

import org.aventyrs.core.character.services.CharacterSkillService;
import org.aventyrs.core.modifier.ModifierResolver;
import org.aventyrs.core.skill.AbstractSkillInteraction;
import org.aventyrs.core.skill.SkillType;

/**
 * Requests a Persuasão Perícia test. Which of Persuasão's specializations the roll is for
 * doesn't change the bonus — see {@link PersuasaoSpecialization} — so it isn't tracked here.
 * The rules text also has an opposed Atenção roll resist Persuasão; that resolution is left
 * to a layer above this core, same as every other cross-character contest. See {@link
 * AbstractSkillInteraction} for how the roll bonus/difficultyReduction are actually computed.
 */
public class PersuasaoInteraction extends AbstractSkillInteraction {

    public PersuasaoInteraction() {
        super(SkillType.PERSUASAO);
    }

    public PersuasaoInteraction(final CharacterSkillService characterSkillService, final ModifierResolver modifierResolver) {
        super(SkillType.PERSUASAO, characterSkillService, modifierResolver);
    }
}
