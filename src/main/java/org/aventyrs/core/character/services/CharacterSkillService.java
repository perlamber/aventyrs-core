package org.aventyrs.core.character.services;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.race.Race;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.util.RollErrorException;

public interface CharacterSkillService {
    public int getValueForRoll(final CharacterSkill characterSkill, final CharacterAttributes characterAttributes, final Race race) throws RollErrorException;

    /**
     * Same as {@link #getValueForRoll(CharacterSkill, CharacterAttributes, Race)}, but uses
     * {@code substituteAttributeDomain} in place of the Skill's own
     * {@code getAttributeDomain()} when non-null — for a character with a
     * {@link org.aventyrs.core.skill.SkillCompetencyAbility#getSubstituteAttributeDomain()}
     * match, e.g. {@code AtaqueCorpoACorpoCompetencyAbility.ACUIDADE}. The caller (a
     * {@code <Skill>Interaction}) is responsible for finding that match, since only it knows
     * which {@code SkillType} its own roll is for.
     */
    public int getValueForRoll(final CharacterSkill characterSkill, final CharacterAttributes characterAttributes, final Race race, final AttributeDomain substituteAttributeDomain) throws RollErrorException;
}
