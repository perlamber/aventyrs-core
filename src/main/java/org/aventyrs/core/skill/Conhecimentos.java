package org.aventyrs.core.skill;

import org.aventyrs.core.character.AttributeDomain;

public class Conhecimentos extends BasicSkill implements Skill {
    public Conhecimentos()
    {
        super(AttributeDomain.GNOSE);
    }

    @Override
    public AttributeDomain getAttributeDomain() {
        return attributreUsed;
    }
}
