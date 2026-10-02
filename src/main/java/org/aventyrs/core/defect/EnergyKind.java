package org.aventyrs.core.defect;

/**
 * Resistência Atípica's "Escolha entre Energia Elemental (1 único elemento à sua escolha), Energia
 * Profana ou Energia Divina". {@link #ELEMENTAL} owes a further pick of the Elemento; Profana and Divina
 * read as the {@code MagicType} of an incoming Magia.
 */
public enum EnergyKind {
    ELEMENTAL,
    PROFANA,
    DIVINA
}
