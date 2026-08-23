package org.aventyrs.core.character.services;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterStatus;
import org.aventyrs.core.sheet.CombatantSheet;

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
    int getCurrentHitPoints(Character character, CombatantSheet characterSheet);

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

    /**
     * The {@link CharacterStatus} tier characterSheet's owner is in <i>right now</i>, derived
     * from the damage currently on the sheet — {@code getMaxHitPoints(sheet.getCharacter()) -
     * sheet.getDamageTaken()}, handed to {@link #getStatus(int, int)}.
     *
     * <p>That subtraction is deliberately <b>unclamped</b>, unlike {@link #getCurrentHitPoints},
     * which floors at zero: the negative range is exactly what distinguishes {@code FALLEN} from
     * {@code COMMA} from {@code DEAD}, so clamping here would make those three tiers
     * unreachable.
     *
     * <p>Resolved fresh on every call, never stored. This is the same "recompute on demand from
     * data already in hand" discipline {@code InitiativeEntry#getEffectiveInitiativeValue} uses
     * for a participant's live Iniciativa standing, and it exists for the same reason: a stored
     * copy needs every path that changes Hit Points to remember to refresh it, and the
     * per-Rodada ones ({@code Bleeding}, {@code Withering}), {@code CombatantSheet#heal} and
     * {@code RestService#applyRest} have no service in scope to refresh through.
     *
     * <p>It lives on this service rather than on {@code CombatantSheet} itself — which holds
     * both halves of the input and would read more naturally — because {@code
     * org.aventyrs.core.sheet} does not depend on {@code org.aventyrs.core.character.services},
     * and resolving a maximum needs {@link #getMaxHitPoints}'s Vigor/Life-Multiplier scan.
     * Putting it on the sheet would invert that dependency. The shape to compare it against is
     * {@link #getCurrentHitPoints} above, its sheet-taking sibling — not the "cascading
     * overload" convention, which delegates from a longer signature down to a shorter one; this
     * delegates the other way, up to the pure tier function.
     */
    CharacterStatus getStatus(CombatantSheet characterSheet);
}
