package org.aventyrs.core.rest;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * The four kinds of Rest, each recovering PV/PM/PD as a multiple of the character's
 * reference Attribute (Vigor/Foco/Instinto respectively).
 */
@Getter
@AllArgsConstructor
public enum RestType {
    MINIMO(0.5),
    CURTO(1.0),
    LONGO(2.0),
    TOTAL(3.0);

    private final double attributeMultiplier;
}
