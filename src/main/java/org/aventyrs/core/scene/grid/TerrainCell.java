package org.aventyrs.core.scene.grid;

import lombok.NonNull;
import org.aventyrs.core.skill.DifficultyLevel;

/**
 * One hex of terrain as the Narrador painted it (table ruling, 2026-10-14).
 *
 * @param kind        ground, water, a climbable face or an obstacle
 * @param difficult   whether it is Terreno Difícil
 * @param groundLevel its surface height in UD — where a walker stands, the top of an obstacle or a climbable face, the
 *                    surface of water
 * @param difficulty  the GD of climbing it or swimming it; {@code null} for the default Médio
 */
public record TerrainCell(@NonNull TerrainKind kind, boolean difficult, int groundLevel, DifficultyLevel difficulty) {

    /** Plain ground at the baseline. */
    public static final TerrainCell GROUND = new TerrainCell(TerrainKind.GROUND, false, 0, null);

    /** Plain Terreno Difícil at the baseline — what a hex marked difficult before 0.1.6.16 is. */
    public static final TerrainCell DIFFICULT = new TerrainCell(TerrainKind.GROUND, true, 0, null);

    /** The GD climbing or swimming this hex asks, Médio unless painted otherwise ("iniciado em Médio"). */
    public DifficultyLevel difficultyOrDefault() {
        return difficulty == null ? DifficultyLevel.MEDIUM : difficulty;
    }
}
