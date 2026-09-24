package org.aventyrs.core.scene.grid;

/**
 * What one mover may do on each hex of a board, as {@link MovementPathfinder} asks it: whether a
 * path may run through a hex, whether it may end there, and what entering it costs in UD.
 *
 * <p>Built for a particular mover and movement by {@code
 * org.aventyrs.core.character.services.MovementTerrainService#stepRules} — occupancy, Terreno
 * Difícil and Entre as Pernas are all rules questions, so they are answered there and not in the
 * pathfinder, which only searches.
 */
public interface StepRules {

    /** Whether a path may pass through hex on its way somewhere else. */
    boolean canPass(GridPosition hex);

    /** Whether a path may end on hex. Implies nothing about {@link #canPass}. */
    boolean canStop(GridPosition hex);

    /** Whether hex counts as Terreno Difícil for this mover, whether or not they ignore its cost. */
    boolean isDifficult(GridPosition hex);

    /** UD spent stepping into hex — at least 1. */
    int enterCost(GridPosition hex);

    /**
     * These rules with every Terreno Difícil hex made impassable and unstoppable — an Investida's
     * or a Reposicionar's path, which may not enter one at all.
     */
    default StepRules avoidingDifficultTerrain() {
        StepRules rules = this;
        return new StepRules() {
            @Override
            public boolean canPass(final GridPosition hex) {
                return !rules.isDifficult(hex) && rules.canPass(hex);
            }

            @Override
            public boolean canStop(final GridPosition hex) {
                return !rules.isDifficult(hex) && rules.canStop(hex);
            }

            @Override
            public boolean isDifficult(final GridPosition hex) {
                return rules.isDifficult(hex);
            }

            @Override
            public int enterCost(final GridPosition hex) {
                return rules.enterCost(hex);
            }
        };
    }
}
