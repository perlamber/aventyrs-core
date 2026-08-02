package org.aventyrs.core.skill;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AtaqueCorpoACorpoExcellencyTest {

    @Test
    void everyExcellencyBelongsToAtaqueCorpoACorpo() {
        for (AtaqueCorpoACorpoExcellency excellency : AtaqueCorpoACorpoExcellency.values()) {
            assertEquals(SkillType.ATAQUE_CORPO_A_CORPO, excellency.getSkillType());
        }
    }

    @Test
    void everyExcellencyHasADescription() {
        for (AtaqueCorpoACorpoExcellency excellency : AtaqueCorpoACorpoExcellency.values()) {
            assertFalse(excellency.getDescription().isBlank());
        }
    }

    @Test
    void listHasTheThreeUniversalTiers() {
        assertEquals(3, AtaqueCorpoACorpoExcellency.values().length);
    }

    @Test
    void onlyProdigioReducesDifficulty() {
        for (AtaqueCorpoACorpoExcellency excellency : AtaqueCorpoACorpoExcellency.values()) {
            int expected = excellency == AtaqueCorpoACorpoExcellency.PRODIGIO ? 1 : 0;
            assertEquals(expected, excellency.getDifficultyReduction());
        }
    }

    @Test
    void unlockedByFiltersAtaqueCorpoACorpoExcellencyByGraduation() {
        assertEquals(List.of(), SkillExcellency.unlockedBy(AtaqueCorpoACorpoExcellency.class, 2));
        assertEquals(List.of(AtaqueCorpoACorpoExcellency.FOCADO), SkillExcellency.unlockedBy(AtaqueCorpoACorpoExcellency.class, 5));
        assertEquals(List.of(AtaqueCorpoACorpoExcellency.FOCADO, AtaqueCorpoACorpoExcellency.PRODIGIO, AtaqueCorpoACorpoExcellency.LENDA),
                SkillExcellency.unlockedBy(AtaqueCorpoACorpoExcellency.class, 10));
    }
}
