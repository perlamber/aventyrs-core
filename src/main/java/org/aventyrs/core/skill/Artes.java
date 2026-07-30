package org.aventyrs.core.skill;

import org.aventyrs.core.character.AttributeDomain;

public class Artes extends BasicSkill implements Skill {
    public Artes()
    {
        super(AttributeDomain.CHARISMA);
    }

    @Override
    public AttributeDomain getAttributeDomain() {
        return attributreUsed;
    }
}
