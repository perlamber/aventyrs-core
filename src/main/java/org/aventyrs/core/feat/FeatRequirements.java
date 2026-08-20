package org.aventyrs.core.feat;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.skill.SkillType;

import lombok.Builder;
import lombok.Getter;

@Builder
public record FeatRequirements (
        AttributeDomain attributeDomain,
        int requiredAttributeValue,
        SkillType requiredSkillType,
        int requiredSkillGraduation,
        Feat requiredFeat
) {}
