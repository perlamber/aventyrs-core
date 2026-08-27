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

    /**
     * How many Dano Base scale-ups this Talento grants character right now — e.g. {@code
     * ArtesMarciaisFeat#ARTISTA_MARCIAL}'s "+1, cumulativamente +1 para cada Título Aventyr
     * Desperto". Summed by {@code
     * org.aventyrs.core.character.services.DamageBaseService#getDamageBase} across {@code
     * Character#getFeats()}. Zero by default; only override on a constant whose rules text
     * raises Dano Base.
     *
     * <p>A scale-up is a row of {@link org.aventyrs.core.character.DamageBase}'s table, never a
     * flat addend — see that type's javadoc for why "+1 Dano Base" and "+1 aos Danos" are
     * different mechanics that must not be summed together.
     */
    default int resolveDamageBaseIncrease(final Character character) {
        return 0;
    }

    private static int graduationOf(final Character character, final SkillType skillType) {
        CharacterSkill characterSkill = character.getSkills().get(skillType);
        return characterSkill == null ? 0 : characterSkill.getGraduation().getGraduationValue();
    }
}
