package org.aventyrs.core.skill;

import org.aventyrs.core.character.services.CharacterSkillService;
import org.aventyrs.core.modifier.ModifierResolver;

/**
 * Requests a Medicina e Cura Perícia test. Which of Medicina e Cura's specializations the
 * roll is for doesn't change the bonus — see {@link MedicinaECuraSpecialization} — so it
 * isn't tracked here. See {@link AbstractSkillInteraction} for how the roll
 * bonus/difficultyReduction are actually computed.
 */
public class MedicinaECuraInteraction extends AbstractSkillInteraction {

    public MedicinaECuraInteraction() {
        super(SkillType.MEDICINA_E_CURA);
    }

    public MedicinaECuraInteraction(final CharacterSkillService characterSkillService, final ModifierResolver modifierResolver) {
        super(SkillType.MEDICINA_E_CURA, characterSkillService, modifierResolver);
    }
}
