package org.aventyrs.core.skill.profissao;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.skill.BasicSkill;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillType;

public class Profissao extends BasicSkill implements Skill {
    public Profissao()
    {
        super(AttributeDomain.GNOSE, SkillType.PROFISSAO);
    }

    @Override
    public AttributeDomain getAttributeDomain() {
        return attributreUsed;
    }
}
