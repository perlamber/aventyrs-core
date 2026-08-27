package org.aventyrs.core.character;

/**
 * The specific elements a {@link DamageType#ELEMENTAL}/{@link DamageType#FISICO_ELEMENTAL} hit
 * is further classified as — meaningless paired with {@link DamageType#FISICO}/{@link
 * DamageType#MAGICO}/{@link DamageType#PRIMORDIAL}, see {@link DamageBonus}. Starting set per
 * this ruleset's currently-named elements; add more constants here as further ones show up.
 */
public enum Element {
    FOGO,
    AGUA,
    MAGMA,
    ROCHA,
    TERRA,
    GELO
}
