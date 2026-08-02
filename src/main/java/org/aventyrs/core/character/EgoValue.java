package org.aventyrs.core.character;

import lombok.Builder;
import lombok.Getter;

/**
 * An Ego's value is the sum of two components: the Base assigned during character
 * creation — 2 in every Ego, plus a single extra point the player places (see
 * {@link org.aventyrs.core.character.services.CharacterCreationService#allocateEgos}) —
 * and a Variable component granted afterwards by Talentos, Títulos Aventyrs, Habilidades
 * or Narrador-awarded points. Only the Base counts toward unlocking a Vantagem de Ego
 * (e.g. {@link org.aventyrs.core.ego.AutocontroleAdvantage}); Base is therefore never
 * touched outside character creation in this model.
 */
@Builder(toBuilder = true)
@Getter
public class EgoValue {
    @Builder.Default
    private int base = 2;
    @Builder.Default
    private int variable = 0;

    public int getTotal() {
        return base + variable;
    }
}
