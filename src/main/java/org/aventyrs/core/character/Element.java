package org.aventyrs.core.character;

/**
 * The specific elements a {@link DamageType#ELEMENTAL} or Mágico Elemental hit
 * is further classified as — meaningless paired with any other {@link DamageType}, see {@link DamageBonus}. Starting set per
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
