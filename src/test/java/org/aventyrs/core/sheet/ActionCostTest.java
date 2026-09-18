package org.aventyrs.core.sheet;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActionCostTest {

    @Test
    void ofActionPointsReportsThatManySpentPoints() {
        ActionCost twoPoints = ActionCost.ofActionPoints(2);

        assertEquals(ActionCost.Kind.FIXED, twoPoints.kind());
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
        assertThrows(IllegalOperationException.class, () -> new ActionCost(ActionCost.Kind.FIXED, 0));
    }

    @Test
    void aPassiveSpendsNothingAndIsNotAnAcaoLivre() {
        assertEquals(ActionCost.Kind.NONE, ActionCost.NONE.kind());
        assertEquals(0, ActionCost.NONE.spentActionPoints());
        assertEquals(0, ActionCost.NONE.minimum());
        // Distinct values: an Ação Livre is a player-triggered action that costs no PA, a passive
        // is no action at all. isPassive() on a Título ability turns on exactly this difference.
        assertNotEquals(ActionCost.NONE, ActionCost.FREE_ACTION);
    }

    @Test
    void aPassiveCarryingPointsIsRejected() {
        assertThrows(IllegalOperationException.class, () -> new ActionCost(ActionCost.Kind.NONE, 1));
    }

    @Test
    void aDynamicCostCarriesItsMinimum() {
        ActionCost variable = ActionCost.dynamic(2);

        assertEquals(ActionCost.Kind.DYNAMIC, variable.kind());
        assertEquals(2, variable.minimum());
        assertTrue(variable.isDynamic());
        assertFalse(ActionCost.ofActionPoints(2).isDynamic());
    }

    @Test
    void aDynamicMinimumBelowOneIsRejected() {
        assertThrows(IllegalOperationException.class, () -> ActionCost.dynamic(0));
        assertThrows(IllegalOperationException.class, () -> ActionCost.dynamic(-1));
    }

    @Test
    void aDynamicCostAcceptsItsMinimumAndAnythingAbove() {
        ActionCost variable = ActionCost.dynamic(2);

        assertFalse(variable.accepts(1));
        assertTrue(variable.accepts(2));
        assertTrue(variable.accepts(99));
    }

    @Test
    void aFixedCostAcceptsOnlyItsOwnAmount() {
        ActionCost fixed = ActionCost.ofActionPoints(2);

        assertFalse(fixed.accepts(1));
        assertTrue(fixed.accepts(2));
        assertFalse(fixed.accepts(3));
    }

    @Test
    void theCostsThatSpendNoPointsAcceptOnlyZero() {
        for (ActionCost cost : List.of(ActionCost.NONE, ActionCost.FREE_ACTION, ActionCost.REACTION)) {
            assertTrue(cost.accepts(0));
            assertFalse(cost.accepts(1));
        }
    }

    @Test
    void resolvingADynamicCostTurnsItIntoTheFixedAmountChosen() {
        assertEquals(ActionCost.ofActionPoints(4), ActionCost.dynamic(2).resolve(4));
        assertEquals(ActionCost.ofActionPoints(2), ActionCost.dynamic(2).resolve(2));
    }

    @Test
    void resolvingACostToAnAmountItRefusesThrows() {
        assertThrows(IllegalOperationException.class, () -> ActionCost.dynamic(2).resolve(1));
        assertThrows(IllegalOperationException.class, () -> ActionCost.ofActionPoints(2).resolve(3));
        assertThrows(IllegalOperationException.class, () -> ActionCost.REACTION.resolve(1));
    }

    @Test
    void resolvingAnythingButADynamicCostReturnsItUnchanged() {
        assertEquals(ActionCost.ofActionPoints(2), ActionCost.ofActionPoints(2).resolve(2));
        assertEquals(ActionCost.FREE_ACTION, ActionCost.FREE_ACTION.resolve(0));
        assertEquals(ActionCost.NONE, ActionCost.NONE.resolve(0));
    }

    // A Variável cost is a price, not a payment: nobody has chosen an amount yet, so there is no
    // honest answer to "what did this spend" and none is invented.
    @Test
    void aDynamicCostHasNoSpentAmountUntilItIsResolved() {
        assertThrows(IllegalOperationException.class, () -> ActionCost.dynamic(2).spentActionPoints());
        assertEquals(4, ActionCost.dynamic(2).resolve(4).spentActionPoints());
    }

    // The same reason, enforced at the log's own boundary so a Talento reading a recorded
    // action's cost (AssassinoFeat#SAQUE_RELAMPAGO) can never meet an unresolved one.
    @Test
    void theActionLogRefusesADynamicCost() {
        assertThrows(IllegalOperationException.class,
                () -> new CombatantAction(SkillType.ATAQUE_CORPO_A_CORPO, AttributeDomain.STRENGTH,
                        null, ActionCost.dynamic(2), 1, null));
    }

    @Test
    void theActionLogAcceptsEveryResolvedCost() {
        for (ActionCost cost : List.of(ActionCost.NONE, ActionCost.FREE_ACTION, ActionCost.REACTION,
                ActionCost.ofActionPoints(2), ActionCost.dynamic(2).resolve(3))) {
            CombatantAction action = new CombatantAction(SkillType.ATAQUE_CORPO_A_CORPO,
                    AttributeDomain.STRENGTH, null, cost, 1, null);

            assertEquals(cost, action.cost());
        }
    }

    @Test
    void theActionLogStillAcceptsNoCostAtAll() {
        CombatantAction action = new CombatantAction(SkillType.ATAQUE_CORPO_A_CORPO,
                AttributeDomain.STRENGTH, null, null, 1, null);

        assertNull(action.cost());
    }
}
