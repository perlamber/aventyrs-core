package org.aventyrs.core.character.services;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.skill.SkillType;

import java.math.BigDecimal;

public interface SkillGraduationService {
    /** A Perícia's max Graduação is this many times the base of its governing Attribute. */
    int GRADUATION_TO_ATTRIBUTE_BASE_MULTIPLIER = 2;

    /**
     * The highest Graduação skillType can currently reach for character: {@value
     * #GRADUATION_TO_ATTRIBUTE_BASE_MULTIPLIER} times the {@code base} (not the full total —
     * racialBonus/variable don't widen this cap) of whichever Attribute currently governs
     * that Perícia — its own key Attribute, or a substituted one if the character holds a
     * matching {@code SkillCompetencyAbility#getSubstituteAttributeDomain()} (resolved via
     * {@code SkillCompetencyAbility#resolveAttributeDomain}, the same resolution every
     * {@code <Skill>Interaction} already uses for its roll). Recomputed on demand, not
     * cached, since either input (an Attribute upgrade, or acquiring/losing a substituting
     * ability) can change between calls. {@code character.getSkills()} must already contain
     * skillType — this is a cap on an already-trained Perícia's growth, not a training check.
     */
    int getMaxGraduation(Character character, SkillType skillType);

    /**
     * Cost in experience to raise this Perícia's Graduação by one point: half the intended
     * (target) Graduação value.
     */
    BigDecimal getUpgradeCost(CharacterSkill currentSkill);

    /**
     * Raises character's CharacterSkill for skillType by one point, spending
     * {@link #getUpgradeCost} from characterSheet's unused experience — this is a
     * Character-progression action, so it can only happen in the context of a
     * {@link CharacterSheet}.
     *
     * @throws IllegalOperationException if the resulting Graduação would exceed
     *                                    {@link #getMaxGraduation}, or if characterSheet
     *                                    doesn't have enough unused experience
     */
    CharacterSkill upgradeGraduation(Character character, CharacterSheet characterSheet, SkillType skillType) throws IllegalOperationException;
}
