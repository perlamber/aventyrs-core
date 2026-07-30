package org.aventyrs.core.skill;

import org.aventyrs.core.character.AttributeDomain;

public interface Skill {
    /** Penalty applied to a Perícia roll's bonus when the character never trained it. */
    int UNTRAINED_PENALTY = -2;

    public AttributeDomain getAttributeDomain();
}
