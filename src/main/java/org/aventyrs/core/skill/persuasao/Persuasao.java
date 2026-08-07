package org.aventyrs.core.skill.persuasao;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.skill.BasicSkill;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillType;

public class Persuasao extends BasicSkill implements Skill {
    public Persuasao()
    {
        super(AttributeDomain.CHARISMA, SkillType.PERSUASAO);
    }

    @Override
    public AttributeDomain getAttributeDomain() {
        return attributreUsed;
    }
}
