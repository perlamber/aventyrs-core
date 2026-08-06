package org.aventyrs.core.character.services;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.sheet.CharacterSheet;

public interface HitPointsService {
    int DEFAULT_LIFE_MULTIPLIER = 4;
    int BASE_HIT_POINTS = 10;

    /**
     * The Life Multiplier, base {@value #DEFAULT_LIFE_MULTIPLIER}, increased by sources such
     * as the Sobre-humano Vigor ability.
     */
    int getLifeMultiplier(Character character);

    /**
     * Total (maximum) Hit Points: Vigor's total value times the Life Multiplier.
     */
    int getMaxHitPoints(Character character);

    /**
     * Current Hit Points: the maximum minus the damage accumulated on the character's sheet,
     * never below zero.
     */
    int getCurrentHitPoints(Character character, CharacterSheet characterSheet);
}
