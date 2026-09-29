package org.aventyrs.core.defect;

/**
 * A Qualidade's class. "Todas as Qualidades Maiores possuem implicitamente, de forma cumulativa, os
 * efeitos da Qualidade Menor de seu tipo", so a {@link #MAIOR} holds both levels' effects.
 */
public enum QualityClass {
    MENOR(1),
    MAIOR(2);

    /** Talentos Gerais traded for one at creation — "1 Talento Geral por uma Qualidade Menor, ou 2 … Maior". */
    private final int generalFeatSlotsTraded;

    QualityClass(final int generalFeatSlotsTraded) {
        this.generalFeatSlotsTraded = generalFeatSlotsTraded;
    }

    public int getGeneralFeatSlotsTraded() {
        return generalFeatSlotsTraded;
    }
}
