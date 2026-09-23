package org.aventyrs.core.sheet;

/**
 * What a running {@link Frenzy} carries beyond the base state — each one a trait "ativada em
 * conjunto ao Frenesi do Gigante Enfurecido" or switched on while it runs. Read by the clauses that
 * depend on it: Titã Enlouquecido lifts the concentration block and gates Frenesi Arcano, Colosso
 * Enfurecido and Cataclismo Elemental; Berserker's hit reaction only fires while its mode is on.
 */
public enum FrenzyMode {
    /** Especialização Titã Enlouquecido. */
    TITA_ENLOUQUECIDO,
    /** Especialização Berserker. */
    BERSERKER,
    /** Suprema Frenesi Esmeralda. */
    FRENESI_ESMERALDA,
    /** Suprema Cataclismo Elemental — its elements live on {@link Frenzy#getCataclysmElements()}. */
    CATACLISMO_ELEMENTAL
}
