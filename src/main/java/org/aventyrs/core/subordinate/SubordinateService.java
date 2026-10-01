package org.aventyrs.core.subordinate;

import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CombatantSheet;

import java.util.UUID;

/**
 * Taking a Subordinado under command and letting one go (core 0.0.92) — "Cada Personagem jogador pode ter sob seu
 * comando uma quantidade de Subordinados igual ao seu Carisma, e não podem possuir Subordinados do mesmo tipo, a menos
 * que um deles seja prodigioso."
 */
public interface SubordinateService {

    /**
     * Puts subordinate under commander's command and grants what is granted once (a Rei's Ego points, to the commander
     * and — when Prodigioso — to every ally character in sceneContext).
     *
     * @throws org.aventyrs.core.sheet.IllegalOperationException {@code SUBORDINATE_LIMIT_REACHED} at the Carisma limit;
     *         {@code SUBORDINATE_GRADE_HELD} for a second common Subordinado of a grade already commanded
     */
    void command(CombatantSheet commander, Subordinate subordinate, SceneContext sceneContext);

    /** Ends the Subordinado with id, if commander commands it. */
    void dismiss(CombatantSheet commander, UUID id);

    /** A Descanso Longo renews every Rei's Ego points — "renovando os bônus … após … Descansos Longos". */
    void renewAfterLongRest(CombatantSheet commander);
}
