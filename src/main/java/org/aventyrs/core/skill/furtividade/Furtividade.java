package org.aventyrs.core.skill.furtividade;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.skill.BasicSkill;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillType;

public class Furtividade extends BasicSkill implements Skill {
    public Furtividade()
    {
        super(AttributeDomain.DEXTERITY, SkillType.FURTIVIDADE);
    }

    @Override
    public AttributeDomain getAttributeDomain() {
        return attributreUsed;
    }
}
