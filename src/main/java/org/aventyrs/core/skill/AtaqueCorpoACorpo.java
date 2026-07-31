package org.aventyrs.core.skill;

import org.aventyrs.core.character.AttributeDomain;

public class AtaqueCorpoACorpo extends BasicSkill implements Skill {
    public AtaqueCorpoACorpo()
    {
        super(AttributeDomain.STRENGTH);
    }

    @Override
    public AttributeDomain getAttributeDomain() {
        return attributreUsed;
    }
}
