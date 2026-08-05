package org.aventyrs.core.skill.medicinaecura;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.skill.BasicSkill;
import org.aventyrs.core.skill.Skill;

public class MedicinaECura extends BasicSkill implements Skill {
    public MedicinaECura()
    {
        super(AttributeDomain.GNOSE);
    }

    @Override
    public AttributeDomain getAttributeDomain() {
        return attributreUsed;
    }
}
