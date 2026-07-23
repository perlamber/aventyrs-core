package org.aventyrs.core.character;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillGraduation;

import java.util.Objects;

@Getter
@Builder(toBuilder = true)
@AllArgsConstructor
public class CharacterSkill{

    private Skill skill;
    private String specialization;
    @Builder.Default
    private SkillGraduation graduation = SkillGraduation.INITIAL_BUILDER.build();

    public void increaseGraduation(int value)
    {
        graduation.increaseGraduation(value);
    }
    public SkillGraduation getGraduation(){
        return graduation;
    }


    public CharacterSkill(Skill skill) {
        Objects.nonNull(skill);
        this.skill = skill;
    }

}
