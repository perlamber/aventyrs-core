package org.aventyrs.core.skill.empatiaselvagem;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.skill.BasicSkill;
import org.aventyrs.core.skill.Skill;

public class EmpatiaSelvagem extends BasicSkill implements Skill {
    public EmpatiaSelvagem()
    {
        super(AttributeDomain.CHARISMA);
    }

    @Override
    public AttributeDomain getAttributeDomain() {
        return attributreUsed;
    }
}
