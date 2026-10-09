package org.aventyrs.core.character.services;

import org.aventyrs.core.ability.PeritoTeoricoAbility;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.util.RollErrorException;

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;

import static org.aventyrs.core.util.TranslatableMessages.SKILL_GRADUATION_AT_MAXIMUM;

public class SkillGraduationServiceImpl implements SkillGraduationService {

    @Override
    public int getMaxGraduation(final Character character, final SkillType skillType) {
        CharacterSkill characterSkill = character.getSkills().get(skillType);
        // An untrained Perícia still has a cap — the one it would grow under once trained.
        AttributeDomain defaultDomain = characterSkill != null
                ? characterSkill.getSkill().getAttributeDomain()
                : skillType.newSkillInstance().getAttributeDomain();
        AttributeDomain peritoTeoricoDomain = PeritoTeoricoAbility.resolveAttributeDomain(character.getAttributeAbilities(), skillType, defaultDomain);
        AttributeDomain governingDomain = SkillCompetencyAbility.resolveAttributeDomain(
                SkillCompetencyAbility.allFor(character), skillType, peritoTeoricoDomain);

        try {
            AttributeValue attributeValue = (AttributeValue) governingDomain.getKeyAttributeMethod().invoke(character.getAttributes());
            return attributeValue.getBase() * GRADUATION_TO_ATTRIBUTE_BASE_MULTIPLIER;
        } catch (RuntimeException | IllegalAccessException | InvocationTargetException e) {
            throw new RollErrorException();
        }
    }

    @Override
    public BigDecimal getUpgradeCost(final CharacterSkill currentSkill) {
        int targetGraduation = currentSkill.getGraduation().getGraduationValue() + 1;
        return BigDecimal.valueOf(targetGraduation).divide(BigDecimal.valueOf(2));
    }

    @Override
    public BigDecimal getUpgradeCost(final Character character, final SkillType skillType) {
        CharacterSkill characterSkill = character.getSkills().get(skillType);
        BigDecimal cost = getUpgradeCost(characterSkill);
        int targetGraduation = characterSkill.getGraduation().getGraduationValue() + 1;
        boolean quickLearning = character.getRace() != null && character.getRace().hasQuickLearning()
                && character.getQuickLearningSkills().contains(skillType)
                && targetGraduation >= QUICK_LEARNING_FIRST_GRADUATION
                && targetGraduation <= quickLearningMaxGraduation(character);
        BigDecimal discounted = quickLearning ? cost.subtract(QUICK_LEARNING_DISCOUNT) : cost;
        // Antecedente discounts (Batedor, Escudeiro, Natureza Longínqua) stack with Aprendizado
        // Rápido and may make a Graduação free — a table ruling — but never negative.
        for (org.aventyrs.core.feat.Feat feat : character.getFeats()) {
            discounted = discounted.subtract(feat.resolveGraduationCostReduction(character, skillType, targetGraduation));
        }
        return discounted.max(BigDecimal.ZERO);
    }

    private static int quickLearningMaxGraduation(final Character character) {
        return character.getFeats().stream()
                .map(feat -> feat.resolveQuickLearningMaxGraduation(character))
                .filter(java.util.Objects::nonNull)
                .reduce(QUICK_LEARNING_MAX_GRADUATION, Math::max);
    }

    @Override
    public CharacterSkill upgradeGraduation(final Character character, final CharacterSheet characterSheet, final SkillType skillType) throws IllegalOperationException {
        CharacterSkill characterSkill = character.getSkills().get(skillType);
        int targetGraduation = characterSkill.getGraduation().getGraduationValue() + 1;
        if (targetGraduation > getMaxGraduation(character, skillType)) {
            throw new IllegalOperationException(SKILL_GRADUATION_AT_MAXIMUM);
        }
        characterSheet.useExperience(getUpgradeCost(character, skillType));
        characterSkill.increaseGraduation(1);
        return characterSkill;
    }
}
