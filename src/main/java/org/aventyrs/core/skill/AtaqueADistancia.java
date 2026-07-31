package org.aventyrs.core.skill;

import org.aventyrs.core.character.AttributeDomain;

public class AtaqueADistancia extends BasicSkill implements Skill {
    public AtaqueADistancia()
    {
        super(AttributeDomain.DEXTERITY);
    }

    @Override
    public AttributeDomain getAttributeDomain() {
        return attributreUsed;
    }
}
