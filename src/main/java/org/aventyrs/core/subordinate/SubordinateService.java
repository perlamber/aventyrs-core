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

    /**
     * {@link #command}, the Subordinado lasting until the commander's next Descanso of at least endsAtRest (core 0.0.98)
     * — Agnação Ancestral Superior's Peão, "que te auxiliará até seu próximo Descanso". {@code null} is {@link #command}.
     */
    void command(CombatantSheet commander, Subordinate subordinate, SceneContext sceneContext,
                 org.aventyrs.core.rest.RestType endsAtRest);

    /** The Esquecida's sombra conselheira: "Tempo de Ação 3PA". */
    int SHADOW_COUNSEL_ACTION_POINTS = 3;

    /** "por Concentração +1 Rodada". */
    int SHADOW_COUNSEL_TRAILING_ROUNDS = 1;

    /**
     * Abraçado pela Esquecida, Fundamentalista (core 0.0.98): "Apenas uma vez por Cena … invocar uma sombra
     * conselheira, que te auxilia agindo como um Subordinado Peão, Cavaleiro ou Torre por Concentração +1 Rodada".
     * holder commands it with benefit's grade; its {@link #SHADOW_COUNSEL_ACTION_POINTS}PA is reported, not spent.
     * ⚠️ "uma vez por Cena" is kept per combat ({@code CombatantSheet#markAffectedThisCombat}).
     *
     * @throws org.aventyrs.core.sheet.IllegalOperationException {@code SHADOW_COUNSEL_NOT_HELD} without the
     *         Fundamentalista rung, {@code SHADOW_COUNSEL_GRADE_NOT_ALLOWED} for a Bispo, Rainha or Rei, {@code
     *         SHADOW_COUNSEL_ALREADY_USED} the second time in a Cena, or the {@link #command} refusals
     */
    Subordinate summonShadowCounsel(CombatantSheet holder, SubordinateBenefit benefit, SceneContext sceneContext);

    /** Ends the Subordinado with id, if commander commands it. */
    void dismiss(CombatantSheet commander, UUID id);

    /** A Descanso Longo renews every Rei's Ego points — "renovando os bônus … após … Descansos Longos". */
    void renewAfterLongRest(CombatantSheet commander);
}
