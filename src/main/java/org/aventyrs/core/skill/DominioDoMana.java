package org.aventyrs.core.skill;

import org.aventyrs.core.character.AttributeDomain;

public class DominioDoMana extends BasicSkill implements Skill {
    public DominioDoMana()
    {
        super(AttributeDomain.FOCUS);
    }

    @Override
    public AttributeDomain getAttributeDomain() {
        return attributreUsed;
    }
}
