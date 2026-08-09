package org.aventyrs.core.skill.esquivaeaparar;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.skill.BasicSkill;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillType;

public class EsquivaEAparar extends BasicSkill implements Skill {
    public EsquivaEAparar()
    {
        super(AttributeDomain.DEXTERITY, SkillType.ESQUIVA_E_APARAR);
    }

    @Override
    public AttributeDomain getAttributeDomain() {
        return attributreUsed;
    }
}
