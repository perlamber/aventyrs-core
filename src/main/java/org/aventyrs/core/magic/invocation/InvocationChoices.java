package org.aventyrs.core.magic.invocation;

/**
 * The counts a caster picks before an invocation Magia is priced (core 0.1.7.6) — Canção de Flora's additional and
 * strengthened animals, each costing PM ({@link SpellInvocationService#extraManaCost}). Ignored by every other Magia.
 *
 * @param additionalAnimals   animals beyond the first
 * @param strengthenedAnimals animals strengthened
 */
public record InvocationChoices(int additionalAnimals, int strengthenedAnimals) {

    /** No choice made. */
    public static final InvocationChoices NONE = new InvocationChoices(0, 0);
}
