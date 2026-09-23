package org.aventyrs.core.character.services;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.sheet.CombatantSheet;

public interface MagicPointsService {
    int DEFAULT_MANA_MULTIPLIER = 3;
    int BASE_MAGIC_POINTS = 10;

    /**
     * The Mana Multiplier: {@link Character#getManaMultiplier()}'s own fixed value (editable
     * per character, {@value #DEFAULT_MANA_MULTIPLIER} by default), increased by sources such
     * as the Conexão com o Mana Focus ability.
     */
    int getManaMultiplier(Character character);

    /**
     * Total (maximum) Magic Points: {@value #BASE_MAGIC_POINTS} plus Foco's total value times
     * the Mana Multiplier.
     */
    int getMaxMagicPoints(Character character);

    /**
     * {@link #getManaMultiplier(Character)} plus what sheet holds for a while — a {@code
     * MANA_MULTIPLIER} {@code TemporaryBonus} (Choque de AEther Maior's +1 / −1). Floored at 1.
     * {@code null} sheet is the Character-only figure.
     */
    int getManaMultiplier(Character character, CombatantSheet sheet);

    /** {@link #getMaxMagicPoints(Character)} off the sheet-aware multiplier. */
    int getMaxMagicPoints(Character character, CombatantSheet sheet);

    /**
     * Current Magic Points: the maximum minus the Magic Points spent on the character's
     * sheet, never below zero.
     */
    int getCurrentMagicPoints(Character character, CombatantSheet characterSheet);
}
