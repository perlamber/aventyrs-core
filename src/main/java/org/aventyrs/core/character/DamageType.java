package org.aventyrs.core.character;

/**
 * The kinds of dano this ruleset distinguishes. Purely a classification tag today, carried by
 * {@link DamageBonus} — nothing in this core resolves per-type resistance/mitigation yet
 * (e.g. {@code org.aventyrs.core.character.services.DamageService} has no notion of this at
 * all), so a caller currently gets the type back only for its own bookkeeping/display.
 */
public enum DamageType {
    FISICO,
    MAGICO,
    ELEMENTAL,
    FISICO_ELEMENTAL,
    PRIMORDIAL
}
