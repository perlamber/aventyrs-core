package org.aventyrs.core.character.services;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.CombatantSheet;

import java.util.List;
import java.util.Optional;

/**
 * Levantar-se — "alternar entre Caído e Pronto, tem o Tempo de Ação de 1PA e permite a Reação
 * Defender o Perímetro para personagens qualificados." Caído is open-ended, and this is how it ends.
 *
 * <p>Same shape as {@code ChargeService}: a cost getter, a non-throwing check, and a throwing
 * {@link #standUp} that lifts the Condição and reports who may react ({@code
 * MovementReactionService#getProvokedReactors} with {@code Manoeuvre#LEVANTAR_SE}). The PA are
 * reported, never deducted, and nothing fires the Reação — the caller offers it.
 */
public interface StandUpService {

    /** "Tempo de Ação de 1PA". */
    ActionCost STAND_UP_COST = ActionCost.ofActionPoints(1);

    /**
     * What standing up costs character — {@link #STAND_UP_COST}, or an Ação Livre for a holder of a
     * Talento answering {@code Feat#standsUpAsFreeAction} (Submissão).
     */
    ActionCost getStandUpCost(Character character);

    /** Why sheet may not stand up right now — a {@code TranslatableMessages} key — or empty. */
    Optional<String> refusalToStandUp(CombatantSheet sheet, SceneContext sceneContext);

    /** The non-throwing form of {@link #refusalToStandUp}. */
    default boolean canStandUp(final CombatantSheet sheet, final SceneContext sceneContext) {
        return refusalToStandUp(sheet, sceneContext).isEmpty();
    }

    /**
     * Stands sheet up: lifts Caído and reports the price and the enemies whose Defender o Perímetro
     * it provokes.
     *
     * @throws org.aventyrs.core.sheet.IllegalOperationException with {@link #refusalToStandUp}'s key
     */
    StandUpResult standUp(CombatantSheet sheet, SceneContext sceneContext);

    /** What a Levantar-se cost and who may react to it. */
    record StandUpResult(ActionCost actionCost, List<CombatantSheet> provokedReactors) {
    }
}
