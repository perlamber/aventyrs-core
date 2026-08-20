package org.aventyrs.core.feat;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.skill.SkillType;

public interface Feat {
    FeatCategory getFeatCategory();
    String getDescription();
    FeatRequirements getFeatRequirements();

    /**
     * Whether character currently satisfies every prerequisite named in {@link
     * #getFeatRequirements()} — an Attribute's base reaching {@code requiredAttributeValue}
     * (skipped when {@code attributeDomain} is unset), a Perícia's Graduação reaching {@code
     * requiredSkillGraduation} (skipped when {@code requiredSkillType} is unset; an untrained
     * Perícia reads as Graduação 0, same as everywhere else in this core), and {@code
     * requiredFeat} already being held (skipped when unset). All three are independent — a
     * requirement left unset never blocks eligibility — and, when more than one is set, all
     * must hold at once, mirroring {@code AventyrTitleAbility#isEligible}'s identical
     * combine-every-set-prerequisite shape. Checked by {@code
     * org.aventyrs.core.character.services.FeatService#grantFeat} before granting.
     */
    default boolean isEligible(final Character character) {
        FeatRequirements requirements = getFeatRequirements();

        boolean attributeSatisfied = requirements.attributeDomain() == null
                || character.getAttributes().getAttribute(requirements.attributeDomain()).getBase() >= requirements.requiredAttributeValue();

        boolean skillSatisfied = requirements.requiredSkillType() == null
                || graduationOf(character, requirements.requiredSkillType()) >= requirements.requiredSkillGraduation();

        boolean featSatisfied = requirements.requiredFeat() == null
                || character.getFeats().contains(requirements.requiredFeat());

        return attributeSatisfied && skillSatisfied && featSatisfied;
    }

    private static int graduationOf(final Character character, final SkillType skillType) {
        CharacterSkill characterSkill = character.getSkills().get(skillType);
        return characterSkill == null ? 0 : characterSkill.getGraduation().getGraduationValue();
    }
}
