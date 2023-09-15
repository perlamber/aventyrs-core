package org.aventyrs.core.character;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillGraduation;
import org.aventyrs.core.util.RollErrorException;

import java.lang.reflect.InvocationTargetException;
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
    /**
     * Fetch the final value for a Skill roll,
     * reaches out to the CharacterConstitution and get the KeyAttribute current value, then fetches the race modifier
     * returns CharacterConstitution value + Race Modifier + Skill.graduation
     */
    public int getValueForRoll(final CharacterAttributes characterAttributes, final Race race) throws RollErrorException
    {
        Integer totalValue = 0;
        totalValue +=  getGraduation().getGraduationValue();
        try {
            final AttributeDomain skillDomain = skill.getAttributeDomain();
            Integer baseParameterValue = (Integer) skillDomain.getKeyAttributeMethod().invoke(characterAttributes);
            totalValue += baseParameterValue;
            int raceModifier = (int)Race.class.getMethod(skillDomain.getKeyAttributeMethod().getName()+"Modifier").invoke(race);
            totalValue += raceModifier;
        }catch (RuntimeException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e)
        {
            throw new RollErrorException();
        }
        return totalValue;
    }

    public CharacterSkill(Skill skill) {
        Objects.nonNull(skill);
        this.skill = skill;
    }

}
