package org.aventyrs.core.skill;

import org.aventyrs.core.modifier.ModifierResolver;
import org.aventyrs.core.modifier.ModifierResolverImpl;
import org.aventyrs.core.modifier.ModifierType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AttentionExcellencyTest {

    @Test
    void everyExcellencyBelongsToAttention() {
        for (AttentionExcellency excellency : AttentionExcellency.values()) {
            assertEquals(SkillType.ATTENTION, excellency.getSkillType());
        }
    }

    @Test
    void everyExcellencyHasADescription() {
        for (AttentionExcellency excellency : AttentionExcellency.values()) {
            assertFalse(excellency.getDescription().isBlank());
        }
    }

    @Test
    void listHasTheThreeUniversalTiers() {
        assertEquals(3, AttentionExcellency.values().length);
    }

    @Test
    void onlyProdigioReducesDifficulty() {
        for (AttentionExcellency excellency : AttentionExcellency.values()) {
            int expected = excellency == AttentionExcellency.PRODIGIO ? 1 : 0;
            assertEquals(expected, excellency.getDifficultyReduction());
        }
    }

    @Test
    void unlockedByFiltersAttentionExcellencyByGraduation() {
        assertEquals(List.of(), SkillExcellency.unlockedBy(AttentionExcellency.class, 2));
        assertEquals(List.of(AttentionExcellency.FOCADO), SkillExcellency.unlockedBy(AttentionExcellency.class, 5));
        assertEquals(List.of(AttentionExcellency.FOCADO, AttentionExcellency.PRODIGIO, AttentionExcellency.LENDA),
                SkillExcellency.unlockedBy(AttentionExcellency.class, 10));
    }

    @Test
    void totalDifficultyReductionCountsProdigioOnceUnlocked() {
        assertEquals(0, SkillExcellency.totalDifficultyReduction(AttentionExcellency.class, 5));
        assertEquals(1, SkillExcellency.totalDifficultyReduction(AttentionExcellency.class, 7));
    }

    @Test
    void onlyFocadoGrantsAReactionsModifier() {
        ModifierResolver modifierResolver = new ModifierResolverImpl();
        for (AttentionExcellency excellency : AttentionExcellency.values()) {
            int expected = excellency == AttentionExcellency.FOCADO ? 1 : 0;
            assertEquals(expected, modifierResolver.sumModifiers(excellency, ModifierType.REACTIONS));
        }
    }
}
