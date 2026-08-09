package org.aventyrs.core.skill.conhecimentos;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.skill.BasicSkill;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillType;

public class Conhecimentos extends BasicSkill implements Skill {
    public Conhecimentos()
    {
        super(AttributeDomain.GNOSE, SkillType.CONHECIMENTOS);
    }

    @Override
    public AttributeDomain getAttributeDomain() {
        return attributreUsed;
    }
}
