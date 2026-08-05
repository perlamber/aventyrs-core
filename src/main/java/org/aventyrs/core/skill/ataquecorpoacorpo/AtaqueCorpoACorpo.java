package org.aventyrs.core.skill.ataquecorpoacorpo;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.skill.BasicSkill;
import org.aventyrs.core.skill.Skill;

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
