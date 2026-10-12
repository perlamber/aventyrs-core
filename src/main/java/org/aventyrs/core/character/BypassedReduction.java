package org.aventyrs.core.character;

/**
 * A mitigation stage one hit ignores — narrower than {@code DamageService}'s all-or-nothing {@code
 * ignoreDamageReduction}, for a clause that names which ones (core 0.1.6.9): Benção Vulcana's "Ignora RD do alvo"
 * and its Corrente's "Ignora RE e RM". RA is never one of them — the rules name it as the reduction nothing ignores.
 */
public enum BypassedReduction {
    /** Redução de Dano — plain physical damage. */
    RD,
    /** Resistência Elemental — elemental damage. */
    RE,
    /** Resistência à Magias — magical damage. */
    RM
}
