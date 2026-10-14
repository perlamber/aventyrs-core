package org.aventyrs.core.character.services;

import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.util.DiceRoller;

/**
 * Dano de Queda (acoes-de-combate.txt) — "infligido apenas quando a altura – medida em UD – for igual ou superior à
 * Resistência à Quedas", which is "o dobro da própria Categoria de Tamanho (mínimo 2UD)". The damage is Dano
 * Primordial equal to half the height in excess of that resistance; every full 10UD of excess adds 1d6 and Feridas
 * Dolorosas until the next Descanso; an excess beyond 40UD is instant death.
 *
 * <p>Caller-driven like every other geometry in this core: the caller measures the height (a token's elevation over
 * the hex's ground level, or the GM's figure) and supplies the dice. A fall always ends Caído ("Quando um personagem
 * cai ele sofre Danos de Queda e a Condição Caído"), even one too short to hurt.
 */
public interface FallService {

    /** "(mínimo 2UD)". */
    int MINIMUM_FALL_RESISTANCE = 2;
    /** "o dobro da própria Categoria de Tamanho". */
    int FALL_RESISTANCE_PER_SIZE_CATEGORY = 2;
    /** "Para cada 10UD excedente soma-se 1d6". */
    int EXCESS_PER_EXTRA_DIE = 10;
    /** "quedas de alturas excedentes superiores a 40UD indicam morte instantânea". */
    int LETHAL_EXCESS = 40;

    /** sheet's Resistência à Quedas, in UD. */
    int getFallResistance(CombatantSheet sheet);

    /** What a fall of heightUd would do to sheet, without applying anything or rolling the extra dice. */
    FallResult preview(CombatantSheet sheet, int heightUd);

    /**
     * Drops sheet heightUd: applies the Dano Primordial (unreduced — {@code DamageType#QUEDA}), Feridas Dolorosas until
     * the next Descanso when the excess reaches 10UD, death past 40UD, and Caído.
     *
     * @param dice rolls the extra d6s; may be {@code null} when the fall needs none
     * @throws IllegalArgumentException if heightUd is negative, or extra dice are due and dice is {@code null}
     */
    FallResult fall(CombatantSheet sheet, int heightUd, DiceRoller dice, SceneContext sceneContext);

    /**
     * What a fall did, or would do.
     *
     * @param heightUd       the height fallen
     * @param fallResistance the faller's Resistência à Quedas
     * @param excessUd       height beyond the resistance, 0 when within it
     * @param extraDice      the d6s the excess adds
     * @param damage         the Dano Primordial dealt — half the excess plus the extra dice (in a preview, without them)
     * @param painfulWounds  whether Feridas Dolorosas applies
     * @param lethal         whether the excess beyond 40UD kills outright
     */
    record FallResult(int heightUd, int fallResistance, int excessUd, int extraDice, int damage,
                      boolean painfulWounds, boolean lethal) {
    }
}
