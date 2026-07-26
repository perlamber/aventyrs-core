package org.aventyrs.core.action;

import org.aventyrs.core.character.Character;

public interface ActionPointsService {
    /** Every character starts with 3 Pontos de Ação (PA) before any bonus or profile applies. */
    int DEFAULT_ACTION_POINTS = 3;

    /** Perícia rolls have a fixed cost of 2PA. */
    int SKILL_ROLL_COST = 2;

    /**
     * Total PA available on the given Turn: {@value #DEFAULT_ACTION_POINTS} plus any
     * {@link org.aventyrs.core.modifier.ModifierType#ACTION_POINTS} bonus, adjusted by the
     * character's {@link ActionProfile} for that Turn. turnNumber is 0-based (0 is the
     * character's first Turn/Round). Never negative.
     */
    int getMaxActionPoints(Character character, int turnNumber);

    /**
     * Whether the character has enough PA on the given Turn to pay a Perícia roll's fixed
     * {@value #SKILL_ROLL_COST}PA cost.
     */
    boolean canAffordSkillRoll(Character character, int turnNumber);
}
