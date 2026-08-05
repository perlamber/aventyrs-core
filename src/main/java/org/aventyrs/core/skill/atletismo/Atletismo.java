package org.aventyrs.core.skill.atletismo;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.skill.BasicSkill;
import org.aventyrs.core.skill.Skill;

public class Atletismo extends BasicSkill implements Skill {
    public Atletismo()
    {
        super(AttributeDomain.STRENGTH);
    }

    @Override
    public AttributeDomain getAttributeDomain() {
        return attributreUsed;
    }
}
