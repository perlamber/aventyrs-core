package org.aventyrs.core.combat;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.skill.CriticalResult;

import java.util.List;

/**
 * What {@link AttackDelivery#resolve} reports about one attack the player made — the mirror of
 * {@link IncomingAttackResult}. Purely a report: nothing here has been applied, and even the
 * chain is only assembled.
 *
 * <p>A {@code null} boxed field means "not applicable", which here means no {@code attackRoll}
 * was supplied, so the outcome is undetermined rather than a miss.
 */
@Getter
@Builder
public class DeliveredAttackResult {

    /**
     * The attacker's full Perícia de Ataque {@link InteractionResult} — its {@code
     * skillRollBonus} (including any target-conditioned bonus the newly-lifted 4-arg {@code
     * applyTo} resolved), {@code difficultyReduction}, {@code criticalResult}, {@code
     * damageBonus}, and — on a hit — {@link InteractionResult#getNextInteraction()} set to the
     * head of the assembled stage chain, aimed at the defender.
     */
    private final InteractionResult attackResult;

    /** The attacker's total: their roll bonus plus the dice. The bonus alone with no roll. */
    private final int attackTotal;

    /** What {@link #attackTotal} had to reach — the defender's Defesa. */
    private final int requiredTotal;

    /**
     * By how much the attack beat the Defesa — {@code attackTotal - requiredTotal}, so zero or
     * positive on a hit. What {@link #effectChainTriggered} is judged against. {@code null}
     * without an {@code attackRoll}.
     */
    private final Integer margin;

    /** {@code true} when the attack landed, {@code null} without an {@code attackRoll}. */
    private final Boolean hit;

    /** The attack roll's own critical outcome, or {@code null} without an {@code attackRoll}. */
    private final CriticalResult criticalResult;

    /**
     * Whether this attack's Efeitos Críticos fired — it landed <i>and</i> the roll was an Acerto
     * Crítico. {@code null} without an {@code attackRoll}.
     */
    private final Boolean criticalEffectTriggered;

    /**
     * Whether this attack's Correntes de Efeitos fired — it landed <i>and</i> cleared the Defesa
     * by {@code EffectChainService}'s required margin. Independent of {@link
     * #criticalEffectTriggered}. {@code null} without an {@code attackRoll}.
     */
    private final Boolean effectChainTriggered;

    /**
     * The attacker's own {@code difficultyReduction}, reported but <b>not applied</b> — see
     * {@link AttackDelivery}'s javadoc for the open question behind that.
     */
    private final int unappliedDifficultyReduction;

    /**
     * One entry per {@link DeliveredAttack#getAdditionalTargets()}, in the order they were
     * supplied — empty for every ordinary single-target attack.
     *
     * <p>The primary target's own outcome stays on this class's flat fields rather than joining
     * the list: every attack has exactly one primary, every existing caller already reads it
     * there, and only it carries the {@link #attackResult} the whole attack shares.
     */
    @Singular
    private final List<DeliveredAttackTargetResult> additionalTargetResults;
}
