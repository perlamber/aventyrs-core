package org.aventyrs.core.character;

/**
 * The kinds of dano this ruleset distinguishes, carried by {@link DamageBonus} and passed to
 * {@code DamageService#calculateFinalDamage}.
 *
 * <p><b>Exactly one type changes mitigation today: {@link #MAGICO}</b>, which is what adds RM
 * ({@code DamageService#getTotalMagicReduction}) to the reduction total. Every other value is
 * still a pure classification tag a caller gets back for its own bookkeeping — and RD itself is
 * type-blind, applying whatever the type, though the rules scope it to Dano Físico
 * não-PRIMORDIAL e não-ELEMENTAL. Narrowing RD, and giving ELEMENTAL/PRIMORDIAL their own
 * handling, is the damage-type system CLAUDE.md's gap catalog still lists as missing.
 */
public enum DamageType {
    FISICO,
    MAGICO,
    ELEMENTAL,
    FISICO_ELEMENTAL,
    PRIMORDIAL
}
