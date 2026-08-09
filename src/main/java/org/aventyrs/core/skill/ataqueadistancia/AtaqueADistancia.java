package org.aventyrs.core.skill.ataqueadistancia;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.skill.BasicSkill;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillType;

public class AtaqueADistancia extends BasicSkill implements Skill {
    public AtaqueADistancia()
    {
        super(AttributeDomain.DEXTERITY, SkillType.ATAQUE_A_DISTANCIA);
    }

    @Override
    public AttributeDomain getAttributeDomain() {
        return attributreUsed;
    }
}
