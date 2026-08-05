package org.aventyrs.core.skill.empatiaselvagem;

import org.aventyrs.core.character.services.CharacterSkillService;
import org.aventyrs.core.modifier.ModifierResolver;
import org.aventyrs.core.skill.AbstractSkillInteraction;
import org.aventyrs.core.skill.SkillType;

/**
 * Requests an Empatia Selvagem Perícia test. Which of Empatia Selvagem's specializations the
 * roll is for doesn't change the bonus — see {@link EmpatiaSelvagemSpecialization} — so it
 * isn't tracked here. See {@link AbstractSkillInteraction} for how the roll
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
