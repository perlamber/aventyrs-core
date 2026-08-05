package org.aventyrs.core.skill.esquivaeaparar;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.skill.BasicSkill;
import org.aventyrs.core.skill.Skill;

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
