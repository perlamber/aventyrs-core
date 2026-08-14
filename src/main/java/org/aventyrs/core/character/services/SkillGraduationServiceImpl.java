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
        AttributeDomain defaultDomain = characterSkill.getSkill().getAttributeDomain();
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
    public CharacterSkill upgradeGraduation(final Character character, final CharacterSheet characterSheet, final SkillType skillType) throws IllegalOperationException {
        CharacterSkill characterSkill = character.getSkills().get(skillType);
        int targetGraduation = characterSkill.getGraduation().getGraduationValue() + 1;
        if (targetGraduation > getMaxGraduation(character, skillType)) {
            throw new IllegalOperationException(SKILL_GRADUATION_AT_MAXIMUM);
        }
        characterSheet.useExperience(getUpgradeCost(characterSkill));
        characterSkill.increaseGraduation(1);
        return characterSkill;
    }
}
