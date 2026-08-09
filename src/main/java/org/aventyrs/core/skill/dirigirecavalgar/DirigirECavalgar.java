package org.aventyrs.core.skill.dirigirecavalgar;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.skill.BasicSkill;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillType;

public class DirigirECavalgar extends BasicSkill implements Skill {
    public DirigirECavalgar()
    {
        super(AttributeDomain.DEXTERITY, SkillType.DIRIGIR_E_CAVALGAR);
    }

    @Override
    public AttributeDomain getAttributeDomain() {
        return attributreUsed;
    }
}
