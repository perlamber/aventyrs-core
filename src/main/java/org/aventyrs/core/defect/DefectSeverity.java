package org.aventyrs.core.defect;

import java.math.BigDecimal;

/**
 * A Defeito's gravidade — "são divididas em 3 tipos: Leves, Moderados e Graves". Each carries what
 * Superar it costs: "Defeitos Leves custam 3 EXP, Moderados 5 EXP, Graves 7 EXP".
 */
public enum DefectSeverity {
    LEVE(3),
    MODERADO(5),
    GRAVE(7);

    private final int overcomeCost;

    DefectSeverity(final int overcomeCost) {
        this.overcomeCost = overcomeCost;
    }

    /** The EXP Superar a Defeito of this gravidade costs. */
    public BigDecimal getOvercomeCost() {
        return BigDecimal.valueOf(overcomeCost);
    }
}
