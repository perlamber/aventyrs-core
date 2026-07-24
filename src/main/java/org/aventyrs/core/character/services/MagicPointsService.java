package org.aventyrs.core.character.services;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.sheet.CharacterSheet;

public interface MagicPointsService {
    int DEFAULT_MANA_MULTIPLIER = 3;

    /**
     * The Mana Multiplier, base {@value #DEFAULT_MANA_MULTIPLIER}, increased by sources such
     * as the Conexão com o Mana Focus ability.
     */
    int getManaMultiplier(Character character);

    /**
     * Total (maximum) Magic Points: Foco's total value times the Mana Multiplier.
     */
    int getMaxMagicPoints(Character character);

    /**
     * Current Magic Points: the maximum minus the Magic Points spent on the character's
     * sheet, never below zero.
     */
    int getCurrentMagicPoints(Character character, CharacterSheet characterSheet);
}
