package org.aventyrs.core.character.services;

import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.Race;
import org.aventyrs.core.util.RollErrorException;

public interface CharacterSkillService {
    public int getValueForRoll(final CharacterSkill characterSkill, final CharacterAttributes characterAttributes, final Race race) throws RollErrorException;
}
