package org.aventyrs.core.action;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ActionProfileTest {

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
    void profilesWithoutATurnEffectLeaveActionPointsUnchanged() {
        for (ActionProfile profile : new ActionProfile[]{ActionProfile.CONSCIENCIA_DEFENSIVA,
                ActionProfile.MOVIMENTO_PLANEJADO, ActionProfile.REFLEXOS_RAPIDOS, ActionProfile.ESTRATEGISTA}) {
            for (int turn = 0; turn < 5; turn++) {
                assertEquals(3, profile.adjustActionPoints(3, turn));
            }
        }
    }
}
