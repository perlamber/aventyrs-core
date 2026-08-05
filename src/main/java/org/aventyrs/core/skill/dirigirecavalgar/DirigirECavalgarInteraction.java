package org.aventyrs.core.skill.dirigirecavalgar;

import org.aventyrs.core.character.services.CharacterSkillService;
import org.aventyrs.core.modifier.ModifierResolver;
import org.aventyrs.core.skill.AbstractSkillInteraction;
import org.aventyrs.core.skill.SkillType;

/**
 * Requests a Dirigir e Cavalgar Perícia test. Which of Dirigir e Cavalgar's specializations
 * the roll is for doesn't change the bonus — see {@link DirigirECavalgarSpecialization} — so
 * it isn't tracked here. See {@link AbstractSkillInteraction} for how the roll
 * bonus/difficultyReduction are actually computed.
 */
public class DirigirECavalgarInteraction extends AbstractSkillInteraction {

    public DirigirECavalgarInteraction() {
        super(SkillType.DIRIGIR_E_CAVALGAR);
    }

    public DirigirECavalgarInteraction(final CharacterSkillService characterSkillService, final ModifierResolver modifierResolver) {
        super(SkillType.DIRIGIR_E_CAVALGAR, characterSkillService, modifierResolver);
    }
}
