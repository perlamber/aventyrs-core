package org.aventyrs.core.skill.attention;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.skill.BasicSkill;
import org.aventyrs.core.skill.Skill;


public class Attention extends BasicSkill implements Skill {
    public Attention()
    {
        super(AttributeDomain.INSTINCT);
    }

    @Override
    public AttributeDomain getAttributeDomain() {
        return attributreUsed;
    }
}
