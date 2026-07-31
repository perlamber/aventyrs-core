package org.aventyrs.core.modifier;

/**
 * Registry of the kinds of numeric bonuses abilities, feats, titles or items can grant.
 * Adding a new kind of bonus to the system only requires a new constant here — nothing
 * that resolves or consumes modifiers needs to change.
 */
public enum ModifierType {
    LIFE_MULTIPLIER,
    SIZE_CATEGORY,
    MANA_MULTIPLIER,
    DETERMINATION_MULTIPLIER,
    ACTION_POINTS,
    SKILL_ROLL_COST,
    SKILL_ROLL_BONUS,
    REACTIONS,
    FREE_ACTIONS
}
