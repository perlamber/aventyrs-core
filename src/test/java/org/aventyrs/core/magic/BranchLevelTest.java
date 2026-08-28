package org.aventyrs.core.magic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BranchLevelTest {

    @Test
    void everyLevelIsAtLeastItself() {
        for (BranchLevel level : BranchLevel.values()) {
            assertTrue(level.isAtLeast(level));
        }
    }

    @Test
    void deeperLevelsAreAtLeastShallowerOnes() {
        assertTrue(BranchLevel.MUDA.isAtLeast(BranchLevel.BROTO));
        assertTrue(BranchLevel.FLORESCENTE.isAtLeast(BranchLevel.SEMENTE));
    }

    @Test
    void shallowerLevelsAreNotAtLeastDeeperOnes() {
        assertFalse(BranchLevel.MUDA.isAtLeast(BranchLevel.EMERGENTE));
        assertFalse(BranchLevel.SEMENTE.isAtLeast(BranchLevel.BROTO));
    }

    @Test
    void previousWalksOneRungUpTheLadder() {
        assertEquals(BranchLevel.SEMENTE, BranchLevel.BROTO.previous().orElseThrow());
        assertEquals(BranchLevel.BROTO, BranchLevel.MUDA.previous().orElseThrow());
        assertEquals(BranchLevel.MUDA, BranchLevel.EMERGENTE.previous().orElseThrow());
        assertEquals(BranchLevel.EMERGENTE, BranchLevel.FLORESCENTE.previous().orElseThrow());
    }

    @Test
    void sementeRestsOnNothing() {
        assertTrue(BranchLevel.SEMENTE.previous().isEmpty());
    }

    @Test
    void advancedByWalksDownTheLadder() {
        assertEquals(BranchLevel.BROTO, BranchLevel.SEMENTE.advancedBy(1));
        assertEquals(BranchLevel.EMERGENTE, BranchLevel.SEMENTE.advancedBy(3));
        assertEquals(BranchLevel.SEMENTE, BranchLevel.SEMENTE.advancedBy(0));
    }

    @Test
    void advancedByClampsAtBothEnds() {
        assertEquals(BranchLevel.FLORESCENTE, BranchLevel.SEMENTE.advancedBy(99));
        assertEquals(BranchLevel.SEMENTE, BranchLevel.MUDA.advancedBy(-99));
    }

    @Test
    void manaCostRisesWithDepth() {
        assertEquals(0, BranchLevel.SEMENTE.getManaCost());
        assertEquals(7, BranchLevel.FLORESCENTE.getManaCost());
    }
}
