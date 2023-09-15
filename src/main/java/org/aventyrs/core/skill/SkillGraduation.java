package org.aventyrs.core.skill;

import lombok.Builder;
import lombok.Getter;

@Builder @Getter
public class SkillGraduation {
    public static final SkillGraduation.SkillGraduationBuilder INITIAL_BUILDER = SkillGraduation.builder().graduationValue(1);
    @Builder.Default
    private int graduationValue = 0;

    public void increaseGraduation(int value)
    {
        graduationValue += value;
    }
}
