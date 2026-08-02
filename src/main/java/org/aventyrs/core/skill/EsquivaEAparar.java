package org.aventyrs.core.skill;

import org.aventyrs.core.character.AttributeDomain;

public class EsquivaEAparar extends BasicSkill implements Skill {
    public EsquivaEAparar()
    {
        super(AttributeDomain.DEXTERITY);
    }

    @Override
    public AttributeDomain getAttributeDomain() {
        return attributreUsed;
    }
}
