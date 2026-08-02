package org.aventyrs.core.skill;

import org.aventyrs.core.character.AttributeDomain;

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
