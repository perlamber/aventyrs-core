package org.aventyrs.core.character.services;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.sheet.CharacterSheet;

public interface DeterminationPointsService {
    int DEFAULT_DETERMINATION_MULTIPLIER = 3;
    int BASE_DETERMINATION_POINTS = 10;

    /**
     * The Determination Multiplier, base {@value #DEFAULT_DETERMINATION_MULTIPLIER},
     * increased by any source annotated with {@link org.aventyrs.core.modifier.ModifierType#DETERMINATION_MULTIPLIER}.
     */
    int getDeterminationMultiplier(Character character);

    /**
     * Total (maximum) Determination Points: {@value #BASE_DETERMINATION_POINTS} plus Instinto's
     * total value times the Determination Multiplier.
     */
    int getMaxDeterminationPoints(Character character);

    /**
     * Current Determination Points: the maximum minus the Determination Points spent on the
     * character's sheet, never below zero.
     */
    int getCurrentDeterminationPoints(Character character, CharacterSheet characterSheet);
}
