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

    /** The source a GM-granted Subordinado carries (core 0.1.5.6). */
    String GM_GRANT = "Mestre";

    /**
     * The GM puts a Subordinado under commander's command (core 0.1.5.6; table ruling 2026-10-08): no Carisma limit and
     * no one-per-grade rule — the GM grants as many as they want — and no Duração, until the GM dismisses it. A Rei's Ego
     * points are granted as by {@link #command}. commander may be a monster.
     *
     * @return the Subordinado now held
     */
    Subordinate grant(CombatantSheet commander, SubordinateBenefit benefit, boolean prodigious,
                      SceneContext sceneContext);

    /**
     * Whether {@link #command} would refuse a Subordinado of benefit (core 0.1.5.6) — commander is at the Carisma limit,
     * or it would be a second common one of a grade already commanded. {@link #grant} lands anyway; the GM is warned.
     */
    boolean exceedsLimits(CombatantSheet commander, SubordinateBenefit benefit, boolean prodigious);

    /**
     * What a Prodigioso Rei commanded on another client gives ally (core 0.1.5.6) — its 2 Ego points, as {@link #command}
     * gives the allies in its context. For the client that owns ally and sees the Rei arrive on a stand-in. Non-cumulative
     * per Rei; nothing for a common one or another grade.
     */
    void shareKingsEgo(CombatantSheet ally, Subordinate king);

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
    default void renewAfterLongRest(CombatantSheet commander) {
        renewAfterLongRest(commander, null);
    }

    /**
     * {@link #renewAfterLongRest(CombatantSheet)}, also renewing what each Prodigioso Rei an ally in sceneContext commands
     * gives sheet (core 0.1.5.6). ⚠️ A Rei's points are "apenas uma vez a cada dia": the grant is non-cumulative per
     * Rei, so renewing twice — or a Descanso before the points were spent — tops them back up to 2 and no further.
     */
    void renewAfterLongRest(CombatantSheet sheet, SceneContext sceneContext);
}
