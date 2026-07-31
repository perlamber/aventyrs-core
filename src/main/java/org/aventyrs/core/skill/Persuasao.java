package org.aventyrs.core.skill;

import org.aventyrs.core.character.AttributeDomain;

public class Persuasao extends BasicSkill implements Skill {
    public Persuasao()
    {
        super(AttributeDomain.CHARISMA);
    }

    @Override
    public AttributeDomain getAttributeDomain() {
        return attributreUsed;
    }
}
