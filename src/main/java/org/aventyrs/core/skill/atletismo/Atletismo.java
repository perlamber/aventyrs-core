package org.aventyrs.core.skill.atletismo;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.skill.BasicSkill;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillType;

public class Atletismo extends BasicSkill implements Skill {
    public Atletismo()
    {
        super(AttributeDomain.STRENGTH, SkillType.ATLETISMO);
    }

    @Override
    public AttributeDomain getAttributeDomain() {
        return attributreUsed;
    }
}
