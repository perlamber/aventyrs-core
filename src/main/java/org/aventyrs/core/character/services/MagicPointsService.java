package org.aventyrs.core.character.services;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.sheet.CharacterSheet;

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
     * Current Magic Points: the maximum minus the Magic Points spent on the character's
     * sheet, never below zero.
     */
    int getCurrentMagicPoints(Character character, CharacterSheet characterSheet);
}
