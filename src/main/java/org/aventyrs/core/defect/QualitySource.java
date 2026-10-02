package org.aventyrs.core.defect;

/**
 * How a Qualidade was acquired — "Personagens podem adquirir Qualidades de apenas 2 formas, sendo ambas
 * durante a criação".
 */
public enum QualitySource {
    /** As a creation Defeito's Benefício de Superação. */
    SUPERACAO,
    /**
     * "Em substituição a Talentos Gerais": 1 for a Menor, 2 for a Maior, reducing the starting General
     * slots. Allowed only to a character holding a creation Defeito — no Qualidade without a Defeito (a
     * table ruling).
     */
    GENERAL_FEAT_TRADE
}
