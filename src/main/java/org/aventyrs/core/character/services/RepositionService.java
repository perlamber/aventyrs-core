package org.aventyrs.core.character.services;

import org.aventyrs.core.action.Manoeuvre;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.IllegalOperationException;

/**
 * Reposicionar ({@link Manoeuvre#REPOSICIONAR}) — a {@value #DISTANCE_UD}UD step (widened by {@link
 * #getDistance}'s sources) bought as an Ação
 * Livre, which provokes no movement Reações and is not possible in Terreno Difícil.
 *
 * <p><b>Every figure is a table ruling (2026-09-23)</b>: the rules corpus names the manoeuvre
 * ({@code MobilidadeFeat}, {@code EscudeiroFeat}, the Sandálias do Corredor) but never defines it.
 *
 * <p>{@code ChargeService}'s shape: a non-throwing predicate and a throwing mutator over one
 * refusal helper. Like every movement here it <b>reports</b>: this core holds no positions, so the
 * caller picks a destination up to {@link #begin}'s answer away and moves the token. It does not
 * claim a movement of the Rodada ({@code consumeMovementThisRound}) — it is not bought with Pontos
 * de Ação — but does claim a Reposicionar ({@code consumeRepositionThisRound}), which "sua primeira
 * ação para Reposicionar-se" reads; it does not deduct the Ação Livre, which no ledger here counts.
 *
 * <p><b>Exclusive with moving on Pontos de Ação</b> (table ruling, 2026-09-24): a combatant who has
 * spent Pontos de Ação moving this Turn — an Investida included — may no longer Reposicionar, and
 * one who has Reposicionou may no longer spend them moving. This service refuses the first half off
 * {@code getMovementsTakenThisRound}; {@code ChargeService} and every caller buying a movement with
 * Pontos de Ação refuse the second off {@code getRepositionsTakenThisRound}.
 *
 * <p>Terreno Difícil is judged at the starting space, off the actor's {@code EnvironmentalState};
 * the destination is the caller's, routed with {@code StepRules#avoidingDifficultTerrain()}.
 */
public interface RepositionService {

    /** "1UD". */
    int DISTANCE_UD = 1;

    /** "uma Ação Livre". */
    ActionCost ACTION_COST = ActionCost.FREE_ACTION;

    /**
     * Whether sheet may Reposicionar now. turnNumber is the 0-based Turn the Ações Livres are
     * counted for; sceneContext is their own snapshot ({@code null} reads as ordinary ground).
     */
    boolean canReposition(CombatantSheet sheet, int turnNumber, SceneContext sceneContext);

    /**
     * How far sheet's next Reposicionar reaches, in UD — read, never claimed: {@value #DISTANCE_UD}
     * plus every {@link org.aventyrs.core.modifier.ModifierType#REPOSITION_DISTANCE} source (the
     * sheet's TemporaryBonus, Habilidades de Atributo and de Competência, item Favores and
     * aprimoramentos) and {@code Feat#resolveRepositionDistanceIncrease} (Movimento Rápido's
     * Rodadas Pares +1UD). currentRound is the 0-based Rodada. Never below {@value #DISTANCE_UD}.
     */
    int getDistance(CombatantSheet sheet, int currentRound);

    /**
     * Declares the Reposicionar, returning the UD the caller may move sheet ({@link #getDistance})
     * and claiming it as this Rodada's next Reposicionar.
     *
     * @throws IllegalOperationException if they are forbidden to move ({@code
     *         REPOSITION_MOVEMENT_PREVENTED}), already spent Pontos de Ação moving this Turn ({@code
     *         REPOSITION_AFTER_MOVEMENT}), have no Ação Livre this Turn ({@code
     *         REPOSITION_REQUIRES_FREE_ACTION}), or stand in Terreno Difícil ({@code
     *         REPOSITION_IN_DIFFICULT_TERRAIN})
     */
    int begin(CombatantSheet sheet, int turnNumber, SceneContext sceneContext);
}
