package org.aventyrs.core.skill;

import org.aventyrs.core.character.AttributeDomain;

public class Atletismo extends BasicSkill implements Skill {
    public Atletismo()
    {
        super(AttributeDomain.STRENGTH);
    }

    @Override
    public AttributeDomain getAttributeDomain() {
        return attributreUsed;
    }
}
