package org.aventyrs.core.skill.medicinaecura;

import org.aventyrs.core.character.services.CharacterSkillService;
import org.aventyrs.core.modifier.ModifierResolver;
import org.aventyrs.core.skill.AbstractSkillInteraction;
import org.aventyrs.core.skill.SkillType;

/**
 * Requests a Medicina e Cura Perícia test. Which of Medicina e Cura's specializations ({@link
 * MedicinaECuraSpecialization}) the roll is for doesn't change {@code skillRollBonus}/{@code
 * difficultyReduction} — a held one can still be requested via {@code
 * SkillRoll#getRequestedAbility()}, switching {@code reachedDifficultyLevel} to expert
 * thresholds; see {@link AbstractSkillInteraction} for how that and the roll
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
