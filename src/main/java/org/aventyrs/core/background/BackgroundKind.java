package org.aventyrs.core.background;

/**
 * The two groups of Antecedentes — every character holds exactly one of each, chosen at creation
 * and never changed: "Existem dois grupos de antecedentes, os de Naturalidade e os de Carreira."
 */
public enum BackgroundKind {
    /** Antecedente de Naturalidade — where the character was born, its culture and habits. */
    ORIGIN,
    /** Antecedente de Carreira — what the character studied, trained or did before adventuring. */
    CAREER
}
