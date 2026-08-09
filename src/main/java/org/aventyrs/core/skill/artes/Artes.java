package org.aventyrs.core.skill.artes;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.skill.BasicSkill;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillType;

public class Artes extends BasicSkill implements Skill {
    public Artes()
    {
        super(AttributeDomain.CHARISMA, SkillType.ARTES);
    }

    @Override
    public AttributeDomain getAttributeDomain() {
        return attributreUsed;
    }
}
