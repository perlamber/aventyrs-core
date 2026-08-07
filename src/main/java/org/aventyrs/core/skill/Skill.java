package org.aventyrs.core.skill;

import org.aventyrs.core.character.AttributeDomain;

public interface Skill {
    /** Penalty applied to a Perícia roll's bonus when the character never trained it. */
    int UNTRAINED_PENALTY = -2;

    /**
     * Vantagem's fixed bonus to a Perícia roll — not a reroll/take-higher mechanic, just a
     * flat +2 added to that specific roll.
     */
    int ADVANTAGE_BONUS = 2;

    public AttributeDomain getAttributeDomain();

    SkillType getSkillType();
}
