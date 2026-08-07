package org.aventyrs.core.skill.atletismo;

import org.aventyrs.core.character.services.CharacterSkillService;
import org.aventyrs.core.modifier.ModifierResolver;
import org.aventyrs.core.skill.AbstractSkillInteraction;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.skill.SkillType;

/**
 * Requests an Atletismo Perícia test. Which of Atletismo's specializations ({@link
 * AtletismoSpecialization}) the roll is for doesn't change {@code skillRollBonus}/{@code
 * difficultyReduction} — a held one can still be requested via {@code
 * SkillRoll#getRequestedAbility()}, switching {@code reachedDifficultyLevel} to expert
 * thresholds. If the character has a {@code SkillCompetencyAbility} for this same skill whose {@link
 * SkillCompetencyAbility#getSubstituteAttributeDomain()} isn't empty (e.g. {@code
 * AtletismoCompetencyAbility.ACROBATA}), that Attribute is used in place of Atletismo's
 * normal Força — see {@link AbstractSkillInteraction} for how the roll
 * bonus/difficultyReduction are actually computed.
 */
public class AtletismoInteraction extends AbstractSkillInteraction {

    public AtletismoInteraction() {
        super(SkillType.ATLETISMO);
    }

    public AtletismoInteraction(final CharacterSkillService characterSkillService, final ModifierResolver modifierResolver) {
        super(SkillType.ATLETISMO, characterSkillService, modifierResolver);
    }
}
