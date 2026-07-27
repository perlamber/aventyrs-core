package org.aventyrs.core.action;

import org.aventyrs.core.character.Character;

public interface ActionPointsService {
    /** Every character starts with 3 Pontos de Ação (PA) before any bonus or profile applies. */
    int DEFAULT_ACTION_POINTS = 3;

    /** A Perícia roll costs 2PA before any bonus, malus or profile applies. */
    int DEFAULT_SKILL_ROLL_COST = 2;

    /**
     * Total PA available on the given Turn: the character's own fixed
     * {@link Character#getActionPoints()} counter plus any
     * {@link org.aventyrs.core.modifier.ModifierType#ACTION_POINTS} bonus, adjusted by the
     * character's {@link ActionProfile} for that Turn. turnNumber is 0-based (0 is the
     * character's first Turn/Round). Never negative.
     */
    int getMaxActionPoints(Character character, int turnNumber);

    /**
     * PA cost of a Perícia roll on the given Turn: {@value #DEFAULT_SKILL_ROLL_COST} plus
     * any {@link org.aventyrs.core.modifier.ModifierType#SKILL_ROLL_COST} adjustment —
     * abilities, feats or any other bonus-granting source — then the character's
     * {@link ActionProfile} for that Turn. turnNumber is 0-based. Never negative.
     */
    int getSkillRollCost(Character character, int turnNumber);

    /**
     * Whether the character has enough PA on the given Turn to pay that Turn's
     * {@link #getSkillRollCost(Character, int)}.
     */
    boolean canAffordSkillRoll(Character character, int turnNumber);
}
