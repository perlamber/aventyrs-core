package org.aventyrs.core.skill.profissao;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.skill.BasicSkill;
import org.aventyrs.core.skill.Skill;

public class Profissao extends BasicSkill implements Skill {
    public Profissao()
    {
        super(AttributeDomain.GNOSE);
    }

    @Override
    public AttributeDomain getAttributeDomain() {
        return attributreUsed;
    }
}
