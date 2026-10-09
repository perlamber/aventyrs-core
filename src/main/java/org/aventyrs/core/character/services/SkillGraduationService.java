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
     * ability) can change between calls. A substitution always governs, even when the
     * substitute Atributo's base is the lower one (table ruling, 2026-10-08). skillType need not
     * be trained: an untrained Perícia gets the cap it would grow under, resolved from {@code
     * skillType.newSkillInstance()}'s key Attribute — this is not a training check.
     */
    int getMaxGraduation(Character character, SkillType skillType);

    /**
     * Cost in experience to raise this Perícia's Graduação by one point: half the intended
     * (target) Graduação value.
     */
    BigDecimal getUpgradeCost(CharacterSkill currentSkill);

    /** Aprendizado Rápido: "custam 0.5EXP a menos". */
    BigDecimal QUICK_LEARNING_DISCOUNT = new BigDecimal("0.5");

    /** Aprendizado Rápido reaches "a Segunda e Terceira Graduação" — from the second… */
    int QUICK_LEARNING_FIRST_GRADUATION = 2;

    /** …to the third, unless a Talento extends it ({@code Feat#resolveQuickLearningMaxGraduation}). */
    int QUICK_LEARNING_MAX_GRADUATION = 3;

    /**
     * {@link #getUpgradeCost(CharacterSkill)} for character's skillType, less Aprendizado Rápido's
     * 0.5 EXP when their Raça has it ({@code Race#hasQuickLearning}), skillType is one they recorded
     * for it ({@code Character#getQuickLearningSkills()}), and the target Graduação is from the second
     * up to the highest their Raça and Talentos reach. What {@link #upgradeGraduation} spends.
     */
    BigDecimal getUpgradeCost(Character character, SkillType skillType);

    /**
     * Raises character's CharacterSkill for skillType by one point, spending
     * {@link #getUpgradeCost(Character, SkillType)} from characterSheet's unused experience — this is a
     * Character-progression action, so it can only happen in the context of a
     * {@link CharacterSheet}.
     *
     * @throws IllegalOperationException if the resulting Graduação would exceed
     *                                    {@link #getMaxGraduation}, or if characterSheet
     *                                    doesn't have enough unused experience
     */
    CharacterSkill upgradeGraduation(Character character, CharacterSheet characterSheet, SkillType skillType) throws IllegalOperationException;
}
