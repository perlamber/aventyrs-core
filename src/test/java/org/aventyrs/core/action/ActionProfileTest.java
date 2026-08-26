package org.aventyrs.core.action;

import org.aventyrs.core.scene.SceneContext;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ActionProfileTest {

    private static final SceneContext COMBAT_SCENE =
            new SceneContext(List.of(), List.of(), Map.of(), null, true, 1, false);
    private static final SceneContext NON_COMBAT_SCENE =
            new SceneContext(List.of(), List.of(), Map.of());

    @Test
    void everyProfileHasADescription() {
        for (ActionProfile profile : ActionProfile.values()) {
            assertFalse(profile.getDescription().isBlank());
        }
    }

    @Test
    void listHasTheSixDescribedProfiles() {
        assertEquals(6, ActionProfile.values().length);
    }

    @Test
    void profilesWithoutAnActionPointsEffectLeaveActionPointsUnchanged() {
        for (ActionProfile profile : new ActionProfile[]{ActionProfile.CONSCIENCIA_DEFENSIVA,
                ActionProfile.MOVIMENTO_PLANEJADO, ActionProfile.REFLEXOS_RAPIDOS}) {
            for (int turn = 0; turn < 5; turn++) {
                assertEquals(3, profile.adjustActionPoints(3, turn));
                assertEquals(3, profile.adjustActionPoints(3, turn, COMBAT_SCENE));
            }
        }
    }

    @Test
    void profilesWithoutAReactionEffectLeaveReactionsUnchanged() {
        for (ActionProfile profile : new ActionProfile[]{ActionProfile.CONSCIENCIA_DEFENSIVA,
                ActionProfile.MOVIMENTO_PLANEJADO, ActionProfile.IMPULSIVO, ActionProfile.CALCULISTA}) {
            for (int turn = 0; turn < 5; turn++) {
                assertEquals(1, profile.adjustReactions(1, turn));
                assertEquals(1, profile.adjustReactions(1, turn, COMBAT_SCENE));
            }
        }
    }

    @Test
    void profilesWithoutAFreeActionEffectLeaveFreeActionsUnchanged() {
        for (ActionProfile profile : new ActionProfile[]{ActionProfile.CONSCIENCIA_DEFENSIVA,
                ActionProfile.REFLEXOS_RAPIDOS, ActionProfile.IMPULSIVO, ActionProfile.CALCULISTA}) {
            for (int turn = 0; turn < 5; turn++) {
                assertEquals(1, profile.adjustFreeActions(1, turn));
                assertEquals(1, profile.adjustFreeActions(1, turn, COMBAT_SCENE));
            }
        }
    }

    @Test
    void noProfileChangesSkillRollCostByDefault() {
        for (ActionProfile profile : ActionProfile.values()) {
            for (int turn = 0; turn < 5; turn++) {
                assertEquals(2, profile.adjustSkillRollCost(2, turn));
            }
        }
    }

    // --- Movimento Planejado ------------------------------------------------------------------

    @Test
    void movimentoPlanejadoDeniesEveryFreeActionOnTheFirstTurn() {
        assertEquals(0, ActionProfile.MOVIMENTO_PLANEJADO.adjustFreeActions(1, 0));
        assertEquals(0, ActionProfile.MOVIMENTO_PLANEJADO.adjustFreeActions(3, 0));
    }

    @Test
    void movimentoPlanejadoGrantsOneExtraFreeActionFromTheSecondTurnOnward() {
        assertEquals(2, ActionProfile.MOVIMENTO_PLANEJADO.adjustFreeActions(1, 1));
        assertEquals(2, ActionProfile.MOVIMENTO_PLANEJADO.adjustFreeActions(1, 2));
        assertEquals(2, ActionProfile.MOVIMENTO_PLANEJADO.adjustFreeActions(1, 10));
    }

    @Test
    void movimentoPlanejadoIgnoresWhetherTheSceneIsCombat() {
        assertEquals(0, ActionProfile.MOVIMENTO_PLANEJADO.adjustFreeActions(1, 0, COMBAT_SCENE));
        assertEquals(0, ActionProfile.MOVIMENTO_PLANEJADO.adjustFreeActions(1, 0, NON_COMBAT_SCENE));
        assertEquals(2, ActionProfile.MOVIMENTO_PLANEJADO.adjustFreeActions(1, 1, COMBAT_SCENE));
        assertEquals(2, ActionProfile.MOVIMENTO_PLANEJADO.adjustFreeActions(1, 1, NON_COMBAT_SCENE));
    }

    @Test
    void movimentoPlanejadoTouchesNeitherActionPointsNorReactions() {
        for (int turn = 0; turn < 5; turn++) {
            assertEquals(3, ActionProfile.MOVIMENTO_PLANEJADO.adjustActionPoints(3, turn));
            assertEquals(1, ActionProfile.MOVIMENTO_PLANEJADO.adjustReactions(1, turn));
        }
    }

    // --- Reflexos Rápidos ---------------------------------------------------------------------

    @Test
    void reflexosRapidosGrantsOneExtraReactionEveryRoundRegardlessOfScene() {
        for (int turn = 0; turn < 5; turn++) {
            assertEquals(2, ActionProfile.REFLEXOS_RAPIDOS.adjustReactions(1, turn));
            assertEquals(2, ActionProfile.REFLEXOS_RAPIDOS.adjustReactions(1, turn, COMBAT_SCENE));
            assertEquals(2, ActionProfile.REFLEXOS_RAPIDOS.adjustReactions(1, turn, NON_COMBAT_SCENE));
        }
    }

    // --- Estrategista -------------------------------------------------------------------------

    @Test
    void estrategistaAdjustsAllThreeStatsInsideACenaDeCombate() {
        assertEquals(2, ActionProfile.ESTRATEGISTA.adjustActionPoints(3, 0, COMBAT_SCENE));
        assertEquals(2, ActionProfile.ESTRATEGISTA.adjustReactions(1, 0, COMBAT_SCENE));
        assertEquals(2, ActionProfile.ESTRATEGISTA.adjustFreeActions(1, 0, COMBAT_SCENE));
    }

    @Test
    void estrategistaLeavesEverythingUnchangedOutsideACenaDeCombate() {
        for (SceneContext context : new SceneContext[]{NON_COMBAT_SCENE, null}) {
            assertEquals(3, ActionProfile.ESTRATEGISTA.adjustActionPoints(3, 0, context));
            assertEquals(1, ActionProfile.ESTRATEGISTA.adjustReactions(1, 0, context));
            assertEquals(1, ActionProfile.ESTRATEGISTA.adjustFreeActions(1, 0, context));
        }
    }

    @Test
    void estrategistaAppliesOnEveryRoundNotJustTheFirst() {
        for (int turn = 0; turn < 5; turn++) {
            assertEquals(2, ActionProfile.ESTRATEGISTA.adjustActionPoints(3, turn, COMBAT_SCENE));
            assertEquals(2, ActionProfile.ESTRATEGISTA.adjustReactions(1, turn, COMBAT_SCENE));
            assertEquals(2, ActionProfile.ESTRATEGISTA.adjustFreeActions(1, turn, COMBAT_SCENE));
        }
    }

    // --- The short overloads delegate ---------------------------------------------------------

    @Test
    void theTwoArgOverloadsReadAsOutsideACenaDeCombate() {
        assertEquals(3, ActionProfile.ESTRATEGISTA.adjustActionPoints(3, 0));
        assertEquals(1, ActionProfile.ESTRATEGISTA.adjustReactions(1, 0));
        assertEquals(1, ActionProfile.ESTRATEGISTA.adjustFreeActions(1, 0));
    }
}
