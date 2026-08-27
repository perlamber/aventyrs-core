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

    /**
     * Desvantagem's fixed malus — the exact symmetric counterpart of {@link
     * #ADVANTAGE_BONUS}, a flat -2 to that specific roll, not a reroll/take-lower mechanic.
     * Added once a real, unconditional Desvantagem finally turned up in the ruleset: an
     * Equipamento whose Conjuração column reads "Desvantagem" rather than a number (see
     * {@code org.aventyrs.core.item.ArmorItem#ARMADURA_COMPLETA}). Several *scoped*
     * Desvantagem clauses elsewhere (e.g. {@code org.aventyrs.core.race.Bestial}'s Inocência
     * Selvagem, {@code org.aventyrs.core.title.santo.AbencoadoPelaLuzAbility}) still can't use
     * this — they're each blocked on their own separate gap (this core doesn't track what a
     * roll is *for*, and has no "this one specific delivered attack" transaction), not on the
     * constant being missing.
     */
    int DISADVANTAGE_MALUS = -2;

    public AttributeDomain getAttributeDomain();

    SkillType getSkillType();
}
