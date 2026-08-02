package org.aventyrs.core.skill;

import org.aventyrs.core.character.AttributeDomain;

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
