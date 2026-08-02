package org.aventyrs.core.skill;

import org.aventyrs.core.character.AttributeDomain;

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
