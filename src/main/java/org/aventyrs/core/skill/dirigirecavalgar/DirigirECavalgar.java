package org.aventyrs.core.skill.dirigirecavalgar;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.skill.BasicSkill;
import org.aventyrs.core.skill.Skill;

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
