package org.aventyrs.core.ego;

/**
 * One movement a trait grants on the spot, outside its holder's Pontos de Ação — {@link SorteAdvantage#AS_NA_MANGA}'s
 * "Imediatamente após utilizar um Ponto de Sorte você pode se mover até 2UD, este movimento não provoca Reações e
 * ignora terrenos difíceis" (core 0.1.6.4).
 *
 * <p>A <b>total distance</b>, not a per-Ponto-de-Ação figure: like an Investida's allowance, it is one move with no
 * points left to multiply by, so it never touches Movimento Base. Resolved, never stored — the caller offers it at
 * once ("imediatamente") and the move is the caller's, as every position is: path it with {@code
 * org.aventyrs.core.scene.grid.MovementPathfinder} over {@code StepRules#ignoringDifficultTerrainCost()} when
 * {@link #ignoresDifficultTerrain()}, spend no movement of the Rodada on it, and skip {@code
 * MovementReactionService#getProvokedReactors} when it does not {@link #provokesReactions()}.
 *
 * @param distanceUd               the furthest it reaches, in UD
 * @param ignoresDifficultTerrain  whether a Terreno Difícil hex costs 1UD rather than 2 on it
 * @param provokesReactions        whether enemies may react to it
 * @param source                   the granting trait's name, for the log
 */
public record GrantedMove(int distanceUd, boolean ignoresDifficultTerrain, boolean provokesReactions, String source) {
}
