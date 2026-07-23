package org.aventyrs.core.character.services;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.Race;
import org.aventyrs.core.util.RollErrorException;

import java.lang.reflect.InvocationTargetException;

public class CharacterSkillServiceImpl implements CharacterSkillService{

    /**
     * Fetch the final value for a Skill roll,
     * reaches out to the CharacterConstitution and get the KeyAttribute current value, then fetches the race modifier
     * returns CharacterConstitution value + Race Modifier + Skill.graduation
     */
    public int getValueForRoll(final CharacterSkill characterSkill, final CharacterAttributes characterAttributes, final Race race) throws RollErrorException
    {
        Integer totalValue = 0;
        totalValue +=  characterSkill.getGraduation().getGraduationValue();
        try {
            final AttributeDomain skillDomain = characterSkill.getSkill().getAttributeDomain();
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
}
