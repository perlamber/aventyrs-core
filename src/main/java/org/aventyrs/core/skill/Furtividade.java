package org.aventyrs.core.skill;

import org.aventyrs.core.character.AttributeDomain;

public class Furtividade extends BasicSkill implements Skill {
    public Furtividade()
    {
        super(AttributeDomain.DEXTERITY);
    }

    @Override
    public AttributeDomain getAttributeDomain() {
        return attributreUsed;
    }
}
