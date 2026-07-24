package org.aventyrs.core.character.services;

import org.aventyrs.core.character.Character;

public interface DeterminationPointsService {
    int DEFAULT_DETERMINATION_MULTIPLIER = 3;

    /**
     * The Determination Multiplier, base {@value #DEFAULT_DETERMINATION_MULTIPLIER},
     * increased by any source annotated with {@link org.aventyrs.core.modifier.ModifierType#DETERMINATION_MULTIPLIER}.
     */
    int getDeterminationMultiplier(Character character);

    /**
     * Total (maximum) Determination Points: Instinto's total value times the Determination Multiplier.
     */
    int getMaxDeterminationPoints(Character character);
}
