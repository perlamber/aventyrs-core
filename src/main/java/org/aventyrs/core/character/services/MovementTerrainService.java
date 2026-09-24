package org.aventyrs.core.character.services;

import org.aventyrs.core.scene.EnvironmentalState;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.scene.grid.MovementMap;
import org.aventyrs.core.scene.grid.StepRules;
import org.aventyrs.core.sheet.CombatantSheet;

/**
 * Terreno Difícil and occupancy — what one movement costs and where it may go, for a mover on a
 * {@link MovementMap} the caller supplies (this core holds no positions).
 *
 * <p><b>Difficulty is a per-hex cost, not a stat.</b> Entering a Terreno Difícil hex costs {@value
 * #DIFFICULT_TERRAIN_COST}UD instead of 1 — "reduz o movimento pela metade" priced where it is
 * paid, so a path half through it pays half again, and nothing multiplies Movimento Base (there is
 * still no multiplicative movement stage, and none is needed). Table ruling, 2026-09-23.
 *
 * <p>What counts as Terreno Difícil: a hex the board marks so, and — for a mover whose Título
 * {@code passesThroughEnemySpaces} (Entre as Pernas) — any space an enemy occupies.
 *
 * <p>Who may go where: an ally's space may be passed but not stopped on; an enemy's blocks,
 * unless a held Título lets the mover through (then it costs Terreno Difícil) or lets them share
 * it ({@code AventyrTitle#mayShareSpaceWith}). Passing allies is a reading — no rule says so —
 * made because Entre as Pernas grants passage through <em>enemies</em> specifically.
 */
public interface MovementTerrainService {

    /** UD spent entering one Terreno Difícil hex. */
    int DIFFICULT_TERRAIN_COST = 2;

    /**
     * Whether sheet's movement at movementIndex (0-based within the Rodada, as {@code
     * CombatantSheet#getMovementsTakenThisRound()} numbers them) ignores Terreno Difícil. Scans a
     * Habilidade de Atributo ({@code StrengthAbility#MOVIMENTO_LIVRE}, first movement only), a
     * Talento ({@code MetamorfoseDraculeaFeat}'s Cavalo de Chifres, while worn), the worn equipment
     * (a socketed Rútilo Subterrâneo) and the Raça ({@code Pequenino}'s Sempre Veloz, on land —
     * withheld while environmentalState says flying or half-submerged, and under racial-trait
     * suppression). {@code null} environmentalState reads as {@link EnvironmentalState#ORDINARY}.
     */
    boolean ignoresDifficultTerrain(CombatantSheet sheet, int movementIndex, EnvironmentalState environmentalState);

    /**
     * The {@link StepRules} for mover's movement at movementIndex across map. moverContext is the
     * mover's own snapshot — its enemies decide which occupants block, and its {@code
     * EnvironmentalState} feeds {@link #ignoresDifficultTerrain}. A {@code null} context treats
     * every occupant as an ally.
     */
    StepRules stepRules(CombatantSheet mover, MovementMap map, SceneContext moverContext, int movementIndex);
}
