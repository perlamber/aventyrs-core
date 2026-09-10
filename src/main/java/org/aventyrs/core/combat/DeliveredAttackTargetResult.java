package org.aventyrs.core.combat;

import lombok.Builder;
import lombok.Getter;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.Interaction;

/**
 * What one <b>additional</b> target got out of a multi-target attack — see {@link
 * DeliveredAttackResult#getAdditionalTargetResults()}.
 *
 * <p>It is deliberately smaller than {@link DeliveredAttackResult}. Everything an attack resolves
 * <em>once</em> lives there and is not repeated here: the Perícia roll and its bonuses, the
 * critical outcome, the attack total. One roll is made and compared against each target's own
 * Defesa, so all that differs per target is the comparison and what it triggers.
 *
 * <p>A {@code null} boxed field means the same thing it does on {@link DeliveredAttackResult}: no
 * {@code attackRoll} was supplied, so the outcome is undetermined rather than a miss.
 */
@Getter
@Builder
public class DeliveredAttackTargetResult {

    /** The additional target this outcome is about. */
    private final CombatantSheet defender;

    /** What the attack total had to reach — this defender's own Defesa. */
    private final int requiredTotal;

    /** By how much the attack beat this defender's Defesa. {@code null} without an attack roll. */
    private final Integer margin;

    /** {@code true} when the attack landed on this defender, {@code null} without an attack roll. */
    private final Boolean hit;

    /**
     * Whether this attack's Efeitos Críticos fired against this defender — the roll was an Acerto
     * Crítico (one roll, so the same critical for everyone) <i>and</i> it landed on them.
     */
    private final Boolean criticalEffectTriggered;

    /**
     * Whether this attack's Correntes de Efeitos fired against this defender — it landed on them
     * <i>and</i> cleared <b>their</b> Defesa by the required margin, which is judged per target
     * because both the margin and the threshold are.
     */
    private final Boolean effectChainTriggered;

    /**
     * The head of the stage chain aimed at this defender, or {@code null} when the attack missed
     * them. Assembled exactly like the primary target's, with one difference: its {@code
     * DamageInteraction} is marked {@code halvingDamage()}, since an additional target takes
     * Meio-Dano. The caller feeds it the <em>same</em> dano figure it feeds the primary chain —
     * one attack makes one dano roll, and the halving happens inside, after RD and RA.
     */
    private final Interaction<CombatantSheet> nextInteraction;
}
