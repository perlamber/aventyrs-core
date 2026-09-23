package org.aventyrs.core.character.services;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.sheet.CombatantSheet;

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
     * {@link #getDeterminationMultiplier(Character)} plus what sheet holds for a while — a {@code
     * DETERMINATION_MULTIPLIER} {@code TemporaryBonus} (Faísca de Determinação Maior's +1 / −1).
     * Floored at 1. {@code null} sheet is the Character-only figure.
     */
    int getDeterminationMultiplier(Character character, CombatantSheet sheet);

    /** {@link #getMaxDeterminationPoints(Character)} off the sheet-aware multiplier. */
    int getMaxDeterminationPoints(Character character, CombatantSheet sheet);

    /**
     * Current Determination Points: the maximum minus the Determination Points spent on the
     * character's sheet, never below zero.
     */
    int getCurrentDeterminationPoints(Character character, CombatantSheet characterSheet);
}
