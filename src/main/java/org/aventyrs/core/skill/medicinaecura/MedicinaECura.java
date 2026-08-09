package org.aventyrs.core.skill.medicinaecura;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.skill.BasicSkill;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillType;

public class MedicinaECura extends BasicSkill implements Skill {
    public MedicinaECura()
    {
        super(AttributeDomain.GNOSE, SkillType.MEDICINA_E_CURA);
    }

    @Override
    public AttributeDomain getAttributeDomain() {
        return attributreUsed;
    }
}
