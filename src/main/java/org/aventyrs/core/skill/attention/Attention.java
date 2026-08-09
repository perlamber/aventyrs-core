package org.aventyrs.core.skill.attention;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.skill.BasicSkill;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillType;


public class Attention extends BasicSkill implements Skill {
    public Attention()
    {
        super(AttributeDomain.INSTINCT, SkillType.ATTENTION);
    }

    @Override
    public AttributeDomain getAttributeDomain() {
        return attributreUsed;
    }
}
