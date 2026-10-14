package org.aventyrs.core.character.services;

import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.PreparedAction;
import org.aventyrs.core.sheet.PreparedActionKind;

import java.util.Optional;

/**
 * Preparação (acoes-de-combate.txt 20261013, core 0.1.6.17) — "Tempo de Ação: 3PA. Escolha uma Ação (Rolagem de
 * Perícia específica, Ativação de Habilidade, Conjuração de Magia, etc.) cujo Tempo seja igual ou inferior à 2PA. Você
 * recebe uma Reação adicional para executar a ação escolhida."
 *
 * <p>Table ruling (2026-10-14): the player names the action with its choices fixed; until their own next Turn begins
 * they may fire it once, at any moment of another combatant's Turn, with a Reação of its own — never their ordinary
 * one. The trigger is narrated, never checked; unfired, it lapses. Firing it does not run the action: the caller runs
 * it, priced as a Reação. PA are reported, never deducted.
 *
 * <p>Since core 0.1.6.21 the held action is a real one — a {@link PreparedActionKind} and a key the caller resolves
 * back — and only its Tempo is fixed at preparation. Its targets and options are asked, and its PM/PD/PV paid, when it
 * fires; an unfired Preparação costs nothing beyond its 3PA (table ruling, 2026-10-14). An action that is already a
 * Reação is not preparable.
 */
public interface PreparationService {

    /** "Tempo de Ação: 3PA". */
    ActionCost PREPARATION_COST = ActionCost.ofActionPoints(3);

    /** "cujo Tempo seja igual ou inferior à 2PA". */
    int MAXIMUM_PREPARED_COST = 2;

    /**
     * Why sheet may not prepare an action costing preparedCost — a {@code TranslatableMessages} key — or empty: more
     * than 2PA ({@code PREPARED_ACTION_TOO_COSTLY}), already a Reação ({@code PREPARED_ACTION_NOT_PREPARABLE}), or a
     * Condição refusing the Preparação's own 3PA.
     */
    Optional<String> refusalToPrepare(CombatantSheet sheet, ActionCost preparedCost, SceneContext sceneContext);

    /**
     * Holds the action ready, replacing any earlier Preparação.
     *
     * @param key   the action within kind, as the caller will resolve it back when firing
     * @param label its display name
     *
     * @throws org.aventyrs.core.sheet.IllegalOperationException with {@link #refusalToPrepare}'s key
     */
    PreparedAction prepare(CombatantSheet sheet, PreparedActionKind kind, String key, String label,
                           ActionCost preparedCost, SceneContext sceneContext);

    /** Whether sheet holds a Preparação it may fire now — a Condição refusing Reações refuses this one too. */
    boolean canFire(CombatantSheet sheet, SceneContext sceneContext);

    /**
     * Fires the held Preparação, spending its own Reação, and returns it so the caller runs the action.
     *
     * @throws org.aventyrs.core.sheet.IllegalOperationException ({@code NO_PREPARED_ACTION}) when none may be fired
     */
    PreparedAction fire(CombatantSheet sheet, SceneContext sceneContext);
}
