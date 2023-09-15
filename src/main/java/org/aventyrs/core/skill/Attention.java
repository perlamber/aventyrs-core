package org.aventyrs.core.skill;

import org.aventyrs.core.character.AttributeDomain;


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
