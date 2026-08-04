package org.aventyrs.core.skill;

import org.aventyrs.core.sheet.IllegalOperationException;

import java.util.List;

import static org.aventyrs.core.util.TranslatableMessages.INVALID_SKILL_ROLL;

/**
 * The already-rolled dice behind a Perícia test — always exactly 3 six-sided dice in this
 * ruleset. This core deliberately never rolls dice itself (see the {@code skill}
 * package-info's "What this library computes" section), so the caller (an API layer) rolls
 * physically or via an RNG and hands the individual face values in here — not just their sum,
 * since {@link #getCriticalResult()} depends on which specific faces came up (matching dice
 * at the extremes), not the total alone.
 *
 * <p>{@code dice} is validated at construction — exactly 3 values, each 1-6 — since this is a
 * true system boundary (input from outside this core), unlike internal invariants this
 * codebase otherwise trusts without checking.
 *
 * <p>{@code requestedAbility} is optional: a caller performing this roll specifically *as* one
 * of the character's {@link SkillCompetencyAbility} maneuvers (as opposed to a plain Perícia
 * test) names which one here. {@link AbstractSkillInteraction} then validates the character
 * actually holds it before proceeding — see its own javadoc. {@code null} means "just a plain
 * roll, no specific ability being invoked," and skips that check entirely.
 */
public class SkillRoll {
    private static final int EXPECTED_DICE_COUNT = 3;
    private static final int MIN_FACE_VALUE = 1;
    private static final int MAX_FACE_VALUE = 6;

    private final List<Integer> dice;
    private final SkillCompetencyAbility requestedAbility;

    public SkillRoll(final List<Integer> dice) {
        this(dice, null);
    }

    public SkillRoll(final List<Integer> dice, final SkillCompetencyAbility requestedAbility) {
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
    }

    /** The {@link SkillCompetencyAbility} this roll is being made to invoke, or {@code null} for a plain roll. */
    public SkillCompetencyAbility getRequestedAbility() {
        return requestedAbility;
    }

    /** The sum of all 3 dice — what gets added to the Perícia's own bonus and compared against a GD. */
    public int getTotal() {
        return dice.stream().mapToInt(Integer::intValue).sum();
    }

    /** See {@link CriticalResult} for what each outcome means and how it's detected here. */
    public CriticalResult getCriticalResult() {
        int onesCount = countFace(MIN_FACE_VALUE);
        int sixesCount = countFace(MAX_FACE_VALUE);
        if (onesCount == EXPECTED_DICE_COUNT) {
            return CriticalResult.FALHA_CRITICA_MAIOR;
        }
        if (sixesCount == EXPECTED_DICE_COUNT) {
            return CriticalResult.ACERTO_CRITICO_MAIOR;
        }
        if (onesCount == 2) {
            return CriticalResult.FALHA_CRITICA_MENOR;
        }
        if (sixesCount == 2) {
            return CriticalResult.ACERTO_CRITICO_MENOR;
        }
        return CriticalResult.NONE;
    }

    private int countFace(final int face) {
        return (int) dice.stream().filter(rolled -> rolled == face).count();
    }
}
