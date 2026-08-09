package org.aventyrs.core.character.services;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterStatus;
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
     * Total (maximum) Hit Points: {@value #BASE_HIT_POINTS} plus Vigor's total value times the
     * Life Multiplier.
     */
    int getMaxHitPoints(Character character);

    /**
     * Current Hit Points: the maximum minus the damage accumulated on the character's sheet,
     * never below zero.
     */
    int getCurrentHitPoints(Character character, CharacterSheet characterSheet);

    /**
     * Resolves which {@link CharacterStatus} tier a current/max Hit Points pair falls into.
     * Unlike {@link #getCurrentHitPoints}, {@code currentHitPoints} here is the raw,
     * unclamped value (maximum minus damage taken) — it can go negative, since damage can
     * exceed max Hit Points (see {@code ResourcePool}'s own javadoc) and that negative range
     * is exactly what distinguishes {@code FALLEN}/{@code COMMA}/{@code DEAD}.
     *
     * <ul>
     *   <li>{@code CLEAN}: at full Hit Points ({@code currentHitPoints >= maxHitPoints}).</li>
     *   <li>{@code HIGH_LIFE}: above two thirds of max.</li>
     *   <li>{@code MEDIUM_LIFE}: above one third of max.</li>
     *   <li>{@code LOW_LIFE}: above zero.</li>
     *   <li>{@code FALLEN}: zero or below, but above negative half of max.</li>
     *   <li>{@code COMMA}: negative half of max or below, but above negative max.</li>
     *   <li>{@code DEAD}: negative max or below.</li>
     * </ul>
     */
    CharacterStatus getStatus(int currentHitPoints, int maxHitPoints);
}
