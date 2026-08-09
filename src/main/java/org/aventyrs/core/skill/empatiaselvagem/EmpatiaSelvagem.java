package org.aventyrs.core.skill.empatiaselvagem;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.skill.BasicSkill;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillType;

public class EmpatiaSelvagem extends BasicSkill implements Skill {
    public EmpatiaSelvagem()
    {
        super(AttributeDomain.CHARISMA, SkillType.EMPATIA_SELVAGEM);
    }

    @Override
    public AttributeDomain getAttributeDomain() {
        return attributreUsed;
    }
}
