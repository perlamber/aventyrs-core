package org.aventyrs.core.sheet;

import org.aventyrs.core.skill.CriticalResult;
import org.aventyrs.core.skill.DifficultyLevel;

/**
 * How one recorded {@link CombatantAction} turned out — the roll's verdict, kept alongside the
 * action in the per-Rodada log so a consumer (typically an API history/event store) has the
 * result, not just the attempt.
 *
 * <p>{@link #succeeded} and {@link #margin} stay <b>tri-state</b>: both {@code null} when the
 * roll was made against nothing stated, exactly as {@code InteractionResult#getSucceeded()}
 * does — "nobody said what this was against" is a third answer, not a failure.
 */
public record ActionOutcome(Boolean succeeded, Integer margin,
                            CriticalResult criticalResult, DifficultyLevel reachedDifficultyLevel) {

    /** Projects the roll-verdict fields of result; a {@code null}-safe copy, tri-state preserved. */
    public static ActionOutcome from(final InteractionResult result) {
        return new ActionOutcome(result.getSucceeded(), result.getMargin(),
                result.getCriticalResult(), result.getReachedDifficultyLevel());
    }
}
