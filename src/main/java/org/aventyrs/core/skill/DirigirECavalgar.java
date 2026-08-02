package org.aventyrs.core.skill;

import org.aventyrs.core.character.AttributeDomain;

public class DirigirECavalgar extends BasicSkill implements Skill {
    public DirigirECavalgar()
    {
        super(AttributeDomain.DEXTERITY);
    }

    @Override
    public AttributeDomain getAttributeDomain() {
        return attributreUsed;
    }
}
