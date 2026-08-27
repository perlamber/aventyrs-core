package org.aventyrs.core.combat;

import lombok.Builder;
import lombok.Getter;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.skill.CriticalResult;
import org.aventyrs.core.skill.DifficultyLevel;

/**
 * What {@link AttackReceiver#resolve} reports about one incoming attack: whether it landed,
 * whether the defense roll was a critical, whether it cleared the Corrente de Efeitos threshold
 * — and, wrapped inside, the defender's own {@link InteractionResult} carrying the pre-wired
 * chain of everything the attack triggered.
 *
 * <p>Purely a report: nothing here has been applied. Even the chain is only <i>assembled</i> —
 * the caller drives it with the pipeline loop {@code org.aventyrs.core.effect}'s package-info
 * documents, supplying the damage figure as it goes.
 *
 * <p>Follows {@link InteractionResult}'s own convention: a {@code null} boxed field means "not
 * applicable", which here means the caller supplied no {@code defenseRoll}, so the outcome is
 * genuinely undetermined rather than resolved as a miss.
 */
@Getter
@Builder
public class IncomingAttackResult {

    /**
     * The defender's full Esquiva e Aparar {@link InteractionResult} — the Defesa-adjusted
     * {@code skillRollBonus}, the {@code difficultyReduction}, the {@code
     * reachedDifficultyLevel}, any {@code blessings}/{@code egoGainDomains} the roll triggered,
     * and — on a hit — {@link InteractionResult#getNextInteraction()} set to the head of the
     * assembled stage chain.
     */
    private final InteractionResult defenseResult;

    /**
     * The defender's total: their Defesa-adjusted {@code skillRollBonus} plus the dice. Equal to
     * the bonus alone when no {@code defenseRoll} was supplied.
     */
    private final int defenseTotal;

    /** What {@link #defenseTotal} had to reach to avoid the attack. */
    private final int requiredTotal;

    /**
     * {@link IncomingAttack#getDifficultyLevel()} after the defender's own {@code
     * difficultyReduction} made it easier — the tier {@link #requiredTotal} is derived from.
     */
    private final DifficultyLevel effectiveDifficultyLevel;

    /**
     * {@code true} when the defender avoided the attack, {@code false} when it landed, {@code
     * null} when no {@code defenseRoll} was supplied.
     */
    private final Boolean defended;

    /**
     * By how much the attack beat the defense — {@code requiredTotal - defenseTotal}, so
     * positive on a hit and zero or negative on a successful defense. What {@link
     * #effectChainTriggered} is judged against. {@code null} without a {@code defenseRoll}.
     */
    private final Integer margin;

    /**
     * The defense roll's own Acerto/Falha Crítica, or {@code null} without a {@code defenseRoll}.
     * A convenience mirror of {@code defenseResult.getCriticalResult()}.
     */
    private final CriticalResult criticalResult;

    /**
     * Whether this attack's Efeitos Críticos fired — that is, the attack landed <i>and</i> the
     * defense roll was a Falha Crítica. {@code null} without a {@code defenseRoll}.
     */
    private final Boolean criticalEffectTriggered;

    /**
     * Whether this attack's Correntes de Efeitos fired — that is, the attack landed <i>and</i>
     * cleared the defense by {@code EffectChainService}'s required margin. Independent of {@link
     * #criticalEffectTriggered}: either can be true without the other. {@code null} without a
     * {@code defenseRoll}.
     */
    private final Boolean effectChainTriggered;
}
