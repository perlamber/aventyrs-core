package org.aventyrs.core.skill;

import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.IllegalOperationException;

import java.util.List;

import static org.aventyrs.core.util.TranslatableMessages.INVALID_SKILL_ROLL;

/**
 * The already-rolled dice behind a Perícia test — always exactly 3 six-sided dice in this
 * ruleset. This core deliberately never rolls dice itself (see the {@code skill}
 * package-info's "What this library computes" section), so the caller (an API layer) rolls
 * physically or via an RNG and hands the individual face values in here — not just their sum,
 * since {@link #getCriticalResult(int)} depends on which specific faces came up (matching dice
 * at the extremes), not the total alone.
 *
 * <p>{@code dice} is validated at construction — exactly 3 values, each 1-6 — since this is a
 * true system boundary (input from outside this core), unlike internal invariants this
 * codebase otherwise trusts without checking.
 *
 * <p>{@code requestedAbility} is optional: a caller performing this roll specifically *as* one
 * of the character's held {@link SkillTrait}s — either a {@link SkillCompetencyAbility}
 * maneuver or a {@link SkillSpecialization} — (as opposed to a plain Perícia test) names which
 * one here. {@link AbstractSkillInteraction} then validates the character actually holds it
 * before proceeding — see its own javadoc; when it's a held {@link SkillSpecialization}, the
 * roll's reached {@link DifficultyLevel} is resolved via {@link
 * DifficultyLevel#reachedByAsExpert} instead of {@link DifficultyLevel#reachedBy}. {@code null}
 * means "just a plain roll, no specific trait being invoked," and skips that check entirely.
 *
 * <p>{@code targetValue} is the number this roll has to <b>beat</b> — the Grau de Dificuldade it
 * was made against, already reduced to a plain int. Optional, and {@code null} for a roll made
 * against nothing in particular, in which case {@code InteractionResult#getSucceeded()} stays
 * {@code null} too: "nobody said what this was against" is a different answer from "failed".
 *
 * <p><b>A number, not a {@link DifficultyLevel}.</b> The usual source is {@code
 * DifficultyLevel#getBaseValue()}, but not every GD in the rules is a tier — {@code
 * ConditionType#DEVORADO}'s "GD 10+Vigor" is computed from a creature and lands between them —
 * so the comparison is arithmetic, exactly as {@code
 * org.aventyrs.core.combat.AttackReceiver#resolve} already does for the combat side. Use {@link
 * #against(List, DifficultyLevel)} when the target genuinely is a tier.
 *
 * <p>{@code actionCost} is optional roll <b>metadata</b> — the Pontos de Ação / Ação Livre /
 * Reação this roll's action cost (see {@link org.aventyrs.core.sheet.ActionCost}). Deliberately
 * <em>not</em> a domain-resolution input the way {@code attackSource} is: nothing in {@link
 * AbstractSkillInteraction}'s bonus or GD math reads it except the one cost-conditioned {@code
 * Feat} hook ({@code Feat#resolveAttackCostDifficultyReduction}, for {@code
 * AssassinoFeat#SAQUE_RELAMPAGO}). It is carried so the caller can build a {@code
 * org.aventyrs.core.sheet.CombatantAction} for the per-Rodada log and so a cost-gated Talento
 * can see it. {@code null} means "caller didn't say".
 */
public class SkillRoll {
    private static final int EXPECTED_DICE_COUNT = 3;
    private static final int MIN_FACE_VALUE = 1;
    private static final int MAX_FACE_VALUE = 6;

    /** 1+1+1 — the only combination of 3 dice (each 1-6) that sums to 3. */
    private static final int MAJOR_CRITICAL_FAILURE_TOTAL = 3;

    /** 1+1+2 — the only combination of 3 dice (each 1-6) that sums to 4. */
    private static final int MINOR_CRITICAL_FAILURE_TOTAL = 4;

    private final List<Integer> dice;
    private final SkillTrait requestedAbility;
    private final Integer targetValue;
    private final ActionCost actionCost;

    public SkillRoll(final List<Integer> dice) {
        this(dice, null, null, null);
    }

    public SkillRoll(final List<Integer> dice, final SkillTrait requestedAbility) {
        this(dice, requestedAbility, null, null);
    }

    /**
     * A roll made against a {@link DifficultyLevel} tier — the common case, resolving the tier to
     * its {@link DifficultyLevel#getBaseValue()} so the comparison stays arithmetic.
     */
    public static SkillRoll against(final List<Integer> dice, final DifficultyLevel target) {
        return new SkillRoll(dice, null, target == null ? null : target.getBaseValue());
    }

    public SkillRoll(final List<Integer> dice, final SkillTrait requestedAbility, final Integer targetValue) {
        this(dice, requestedAbility, targetValue, null);
    }

    public SkillRoll(final List<Integer> dice, final SkillTrait requestedAbility, final Integer targetValue,
                     final ActionCost actionCost) {
        if (dice.size() != EXPECTED_DICE_COUNT) {
            throw new IllegalOperationException(INVALID_SKILL_ROLL);
        }
        for (int face : dice) {
            if (face < MIN_FACE_VALUE || face > MAX_FACE_VALUE) {
                throw new IllegalOperationException(INVALID_SKILL_ROLL);
            }
        }
        this.dice = dice;
        this.requestedAbility = requestedAbility;
        this.targetValue = targetValue;
        this.actionCost = actionCost;
    }

    /**
     * The number this roll has to beat, or {@code null} when it was made against nothing stated.
     * See this class's own javadoc for why it is an int rather than a {@link DifficultyLevel}.
     */
    public Integer getTargetValue() {
        return targetValue;
    }

    /**
     * The {@link SkillTrait} (a {@link SkillCompetencyAbility} or a {@link SkillSpecialization})
     * this roll is being made to invoke, or {@code null} for a plain roll.
     */
    public SkillTrait getRequestedAbility() {
        return requestedAbility;
    }

    /**
     * What this roll's action cost — a Pontos de Ação amount, an Ação Livre, or a Reação — or
     * {@code null} when the caller didn't say. See this class's own javadoc for why it is
     * metadata rather than a resolution input.
     */
    public ActionCost getActionCost() {
        return actionCost;
    }

    /** The sum of all 3 dice — what gets added to the Perícia's own bonus and compared against a GD. */
    public int getTotal() {
        return dice.stream().mapToInt(Integer::intValue).sum();
    }

    /** Same as {@link #getCriticalResult(int)} with no Margem Crítica widening applied. */
    public CriticalResult getCriticalResult() {
        return getCriticalResult(0);
    }

    /**
     * See {@link CriticalResult} for what each outcome means. criticalMarginIncrease is the
     * combined "número" widening from every source that grants one right now — see {@link
     * org.aventyrs.core.ability.AttributeAbility#resolveCriticalMarginIncrease}/{@link
     * org.aventyrs.core.ego.EgoAdvantage#resolveCriticalMarginIncrease}/{@link
     * SkillCompetencyAbility#resolveCriticalMarginIncrease} (e.g. {@link
     * org.aventyrs.core.ability.DexterityAbility#LETALIDADE_PROGRESSIVA}), summed by {@link
     * AbstractSkillInteraction} across all three before calling this — a caller with no such
     * source in hand can pass 0, same as the plain-fixed-threshold behavior this method used to
     * have unconditionally. Only widens Acerto Crítico Menor: unlike it, Falha Crítica Menor/
     * Maior and Acerto Crítico Maior are each fixed at one exact dice combination in this
     * ruleset's own text. A clause that widens Acerto Crítico <em>Maior</em> is a separate,
     * explicit mechanism — it does not ride this margin, and none is authored yet.
     *
     * <p>Widening lowers the face value two dice must clear from {@code MAX_FACE_VALUE} down by
     * criticalMarginIncrease (floored at {@code MIN_FACE_VALUE}, and negative increases treated
     * as 0) — e.g. a margin of 1 means two dice showing 5 <em>or</em> 6 now count, matching
     * {@code AtaqueCorpoACorpoCompetencyAbility#ATAQUE_PRECISO}'s own rules text ("5s counting
     * alongside 6s"). Checked with {@code >=}, not {@code ==}: three dice within the widened
     * range (e.g. margin 1 on 6+6+5) still reads as Acerto Crítico Menor, not a non-match — it
     * can never collide with Acerto Crítico Maior, which is checked first and only ever matches
     * a literal three 6s regardless of any margin.
     */
    public CriticalResult getCriticalResult(final int criticalMarginIncrease) {
        int total = getTotal();
        if (total == MAJOR_CRITICAL_FAILURE_TOTAL) {
            return CriticalResult.FALHA_CRITICA_MAIOR;
        }
        if (countFace(MAX_FACE_VALUE) == EXPECTED_DICE_COUNT) {
            return CriticalResult.ACERTO_CRITICO_MAIOR;
        }
        if (total == MINOR_CRITICAL_FAILURE_TOTAL) {
            return CriticalResult.FALHA_CRITICA_MENOR;
        }
        int widenedThreshold = Math.max(MIN_FACE_VALUE, MAX_FACE_VALUE - Math.max(0, criticalMarginIncrease));
        if (countFacesAtOrAbove(widenedThreshold) >= 2) {
            return CriticalResult.ACERTO_CRITICO_MENOR;
        }
        return CriticalResult.NONE;
    }

    private int countFace(final int face) {
        return (int) dice.stream().filter(rolled -> rolled == face).count();
    }

    private int countFacesAtOrAbove(final int face) {
        return (int) dice.stream().filter(rolled -> rolled >= face).count();
    }
}
