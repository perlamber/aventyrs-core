package org.aventyrs.core.skill.ataquecorpoacorpo;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.skill.BasicSkill;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillType;

public class AtaqueCorpoACorpo extends BasicSkill implements Skill {
    public AtaqueCorpoACorpo()
    {
        super(AttributeDomain.STRENGTH, SkillType.ATAQUE_CORPO_A_CORPO);
    }

    @Override
    public AttributeDomain getAttributeDomain() {
        return attributreUsed;
    }
}
