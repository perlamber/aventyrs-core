package org.aventyrs.core.character.services;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.race.Race;
import org.aventyrs.core.util.RollErrorException;

import java.lang.reflect.InvocationTargetException;

public class CharacterSkillServiceImpl implements CharacterSkillService{

    /**
     * Fetch the final value for a Skill roll: the attribute's total (base + racial
     * bonus + variable, already resolved on the character's CharacterAttributes)
     * plus the Skill's graduation.
     */
    public int getValueForRoll(final CharacterSkill characterSkill, final CharacterAttributes characterAttributes, final Race race) throws RollErrorException
    {
        return getValueForRoll(characterSkill, characterAttributes, race, null);
    }

    public int getValueForRoll(final CharacterSkill characterSkill, final CharacterAttributes characterAttributes, final Race race, final AttributeDomain substituteAttributeDomain) throws RollErrorException
    {
        int totalValue = characterSkill.getGraduation().getGraduationValue();
        try {
            final AttributeDomain domain = substituteAttributeDomain != null ? substituteAttributeDomain : characterSkill.getSkill().getAttributeDomain();
            AttributeValue attributeValue = (AttributeValue) domain.getKeyAttributeMethod().invoke(characterAttributes);
            totalValue += attributeValue.getTotal();
        }catch (RuntimeException | IllegalAccessException | InvocationTargetException e)
        {
            throw new RollErrorException();
        }
        return totalValue;
    }
}
