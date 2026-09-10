package org.aventyrs.core.sheet;

import org.aventyrs.core.skill.CriticalResult;
import org.aventyrs.core.skill.DifficultyLevel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ActionOutcomeTest {

    @Test
    void fromCarriesTheRollVerdictFieldsThrough() {
        InteractionResult result = InteractionResult.builder()
                .succeeded(true)
                .margin(4)
                .criticalResult(CriticalResult.ACERTO_CRITICO_MENOR)
                .reachedDifficultyLevel(DifficultyLevel.HARD)
                .build();

        ActionOutcome outcome = ActionOutcome.from(result);

        assertEquals(true, outcome.succeeded());
        assertEquals(4, outcome.margin());
        assertEquals(CriticalResult.ACERTO_CRITICO_MENOR, outcome.criticalResult());
        assertEquals(DifficultyLevel.HARD, outcome.reachedDifficultyLevel());
    }

    @Test
    void fromKeepsSucceededAndMarginTriStateWhenNothingWasStated() {
        InteractionResult result = InteractionResult.builder()
                .criticalResult(CriticalResult.NONE)
                .build();

        ActionOutcome outcome = ActionOutcome.from(result);

        assertNull(outcome.succeeded());
        assertNull(outcome.margin());
        assertEquals(CriticalResult.NONE, outcome.criticalResult());
        assertNull(outcome.reachedDifficultyLevel());
    }
}
