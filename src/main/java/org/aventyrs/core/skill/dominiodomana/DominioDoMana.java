package org.aventyrs.core.skill.dominiodomana;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.skill.BasicSkill;
import org.aventyrs.core.skill.Skill;

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
