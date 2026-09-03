package org.aventyrs.core.sheet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ActionCostTest {

    @Test
    void ofActionPointsReportsThatManySpentPoints() {
        ActionCost twoPoints = ActionCost.ofActionPoints(2);

        assertEquals(ActionCost.Kind.ACTION_POINTS, twoPoints.kind());
        assertEquals(2, twoPoints.spentActionPoints());
    }

    @Test
    void aReacaoAndAnAcaoLivreBothSpendZeroPoints() {
        assertEquals(0, ActionCost.FREE_ACTION.spentActionPoints());
        assertEquals(0, ActionCost.REACTION.spentActionPoints());
    }

    @Test
    void anActionPointCostBelowOneIsRejected() {
        assertThrows(IllegalOperationException.class, () -> ActionCost.ofActionPoints(0));
        assertThrows(IllegalOperationException.class, () -> ActionCost.ofActionPoints(-1));
    }

    @Test
    void aFreeActionOrReactionCarryingPointsIsRejected() {
        assertThrows(IllegalOperationException.class, () -> new ActionCost(ActionCost.Kind.FREE_ACTION, 3));
        assertThrows(IllegalOperationException.class, () -> new ActionCost(ActionCost.Kind.REACTION, 1));
    }

    @Test
    void anActionPointKindWithZeroPointsIsRejected() {
        assertThrows(IllegalOperationException.class, () -> new ActionCost(ActionCost.Kind.ACTION_POINTS, 0));
    }
}
