package org.aventyrs.core.character.services;

import org.aventyrs.core.character.Character;

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
}
