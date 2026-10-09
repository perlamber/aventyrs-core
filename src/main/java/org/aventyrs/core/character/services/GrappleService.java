package org.aventyrs.core.character.services;

import org.aventyrs.core.monster.MonsterSheet;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.skill.SkillType;

import java.util.List;
import java.util.Optional;

/**
 * Agarrar, and the way out of it (core 0.1.5). The rules text defines only the escape, so the
 * manoeuvre itself is a table ruling (2026-10-07):
 *
 * <ul>
 *   <li><b>Agarrar</b> — {@link #GRAB_COST}, an Ataque Corpo-a-Corpo against the target's Defesa.
 *   It deals no damage and triggers no Efeito Crítico or Corrente; success leaves the target
 *   {@code ConditionType#AGARRADO} with the captor as its source. Every character may do it — unless
 *   they have no arms (Membro Ausente), and not even an Arma Natural stands in for them.</li>
 *   <li>The captor needs a <b>free hand</b>, and each hold keeps one occupied ({@link
 *   org.aventyrs.core.item.HandBudget}); the target may be at most {@link #MAXIMUM_SIZE_DIFFERENCE}
 *   Categorias larger.</li>
 *   <li>A hold ends when the captor moves ({@link #releaseAllHeldBy}), lets go ({@link #release}, an
 *   Ação Livre), or can no longer hold — Caído, Imobilizado, Desacordado or at 0 PV or less, which
 *   the sheet itself notices ({@code CombatantSheet#canMaintainGrapple}).</li>
 *   <li><b>Libertar-se do Agarrão</b> and <b>da Imobilização</b> cost {@link #ESCAPE_COST} each.</li>
 * </ul>
 *
 * <p><b>Players roll, foes present GDs</b> — the asymmetry {@code AttackDelivery}/{@code
 * AttackReceiver} already keep, in both directions: a player grabbing a foe rolls Ataque against its
 * Defesa; a foe grabbing a player is resisted by the player's Esquiva e Aparar against the foe's Ataque
 * GD; escaping a foe is the player's Ataque against that same GD; and when a held foe struggles, the
 * player rolls Ataque against it to keep the hold. A contest between two player characters has no
 * resolution yet ({@code OPPOSED_ROLL_UNSUPPORTED}).
 *
 * <p>Like {@code ChargeService}, PA are reported, never deducted, and every roll is the caller's to
 * throw; unlike it, the service resolves its own rolls, because none of them is an ordinary attack.
 */
public interface GrappleService {

    /** Agarrar's Tempo de Ação (table ruling). */
    ActionCost GRAB_COST = ActionCost.ofActionPoints(2);

    /** Libertar-se do Agarrão / da Imobilização (table ruling). */
    ActionCost ESCAPE_COST = ActionCost.ofActionPoints(2);

    /** How many Categorias larger than the captor a target may be — the cap Agarrar e Derrubar keeps. */
    int MAXIMUM_SIZE_DIFFERENCE = org.aventyrs.core.effect.AgarrarEDerrubar.MAXIMUM_SIZE_DIFFERENCE;

    /** Why captor may not grab target right now — one {@code TranslatableMessages} key — or empty. */
    Optional<String> refusalToGrab(CombatantSheet captor, CombatantSheet target, SceneContext sceneContext);

    /** The non-throwing form of {@link #refusalToGrab}. */
    default boolean canGrab(final CombatantSheet captor, final CombatantSheet target, final SceneContext sceneContext) {
        return refusalToGrab(captor, target, sceneContext).isEmpty();
    }

    /**
     * A player grabs a foe: roll is their Ataque Corpo-a-Corpo, against the foe's Defesa Física.
     *
     * @throws org.aventyrs.core.sheet.IllegalOperationException with {@link #refusalToGrab}'s key
     */
    GrappleResult grab(CombatantSheet captor, MonsterSheet target, SceneContext sceneContext, SkillRoll roll);

    /**
     * {@link #grab(CombatantSheet, MonsterSheet, SceneContext, SkillRoll)} with the foe's Defesa Física
     * given rather than read — for a client holding only an identity copy of the foe, which knows its
     * authored Defesa from the bestiary. The foe's own Condições are added on top (Desprevenido, Fraqueza).
     */
    GrappleResult grab(CombatantSheet captor, CombatantSheet target, int targetPhysicalDefense,
                       SceneContext sceneContext, SkillRoll roll);

    /**
     * A foe tries to grab a player: roll is the player's Esquiva e Aparar, against the foe's Ataque
     * GD. Held (or the foe could not have grabbed at all), nothing happens; otherwise the player is
     * Agarrado by the foe.
     */
    GrappleResult defendGrab(CombatantSheet defender, MonsterSheet captor, SceneContext sceneContext, SkillRoll roll);

    /**
     * {@link #defendGrab(CombatantSheet, MonsterSheet, SceneContext, SkillRoll)} with the foe's Ataque GD
     * (base value plus bonus) given rather than read — for a client that holds only an identity copy of
     * the foe and received its GD with the roll request.
     */
    GrappleResult defendGrab(CombatantSheet defender, CombatantSheet captor, int captorAttackGd,
                             SceneContext sceneContext, SkillRoll roll);

    /**
     * Libertar-se do Agarrão — a held player rolls attackSkill (a Perícia de Ataque) against their
     * foe captor's Ataque GD; success leaves them Pronto.
     */
    GrappleResult escape(CombatantSheet held, SceneContext sceneContext, SkillType attackSkill, SkillRoll roll);

    /** {@link #escape(CombatantSheet, SceneContext, SkillType, SkillRoll)} with the captor's Ataque GD given. */
    GrappleResult escape(CombatantSheet held, SceneContext sceneContext, SkillType attackSkill, SkillRoll roll,
                         int captorAttackGd);

    /**
     * A held foe struggles: its captor, a player, rolls Ataque Corpo-a-Corpo against the foe's Ataque
     * GD to keep the hold, and the foe goes free on a failure.
     */
    GrappleResult holdAgainst(CombatantSheet captor, MonsterSheet escapee, SceneContext sceneContext, SkillRoll roll);

    /** {@link #holdAgainst(CombatantSheet, MonsterSheet, SceneContext, SkillRoll)} with the foe's Ataque GD given. */
    GrappleResult holdAgainst(CombatantSheet captor, CombatantSheet escapee, int escapeeAttackGd,
                              SceneContext sceneContext, SkillRoll roll);

    /**
     * Libertar-se da Imobilização — Furtividade, against the Imobilizado's pre-set GD when a material
     * or situation holds them, or a foe captor's Ataque GD.
     */
    GrappleResult escapeImmobilization(CombatantSheet held, SceneContext sceneContext, SkillRoll roll);

    /**
     * {@link #escapeImmobilization(CombatantSheet, SceneContext, SkillRoll)} with the captor's Ataque GD
     * given, used when the Imobilizado has no preset GD of its own.
     */
    GrappleResult escapeImmobilization(CombatantSheet held, SceneContext sceneContext, SkillRoll roll,
                                       int captorAttackGd);

    /** The captor lets target go — an Ação Livre. {@code true} if there was a hold to end. */
    boolean release(CombatantSheet captor, CombatantSheet target);

    /**
     * Ends every hold captor keeps — called by whoever moves the captor ("moving releases", table
     * ruling). Returns who went free.
     */
    List<CombatantSheet> releaseAllHeldBy(CombatantSheet captor);
}
