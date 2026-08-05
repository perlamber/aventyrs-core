package org.aventyrs.core.skill.dominiodomana;

import org.aventyrs.core.skill.SkillExcellency;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class DominioDoManaExcellencyTest {

    @Test
    void everyExcellencyBelongsToDominioDoMana() {
        for (DominioDoManaExcellency excellency : DominioDoManaExcellency.values()) {
            assertEquals(SkillType.DOMINIO_DO_MANA, excellency.getSkillType());
        }
    }

    @Test
    void everyExcellencyHasADescription() {
        for (DominioDoManaExcellency excellency : DominioDoManaExcellency.values()) {
            assertFalse(excellency.getDescription().isBlank());
        }
    }

    @Test
    void listHasTheThreeUniversalTiers() {
        assertEquals(3, DominioDoManaExcellency.values().length);
    }

    @Test
    void onlyProdigioReducesDifficulty() {
        for (DominioDoManaExcellency excellency : DominioDoManaExcellency.values()) {
            int expected = excellency == DominioDoManaExcellency.PRODIGIO ? 1 : 0;
            assertEquals(expected, excellency.getDifficultyReduction());
        }
    }

    @Test
    void unlockedByFiltersDominioDoManaExcellencyByGraduation() {
        assertEquals(List.of(), SkillExcellency.unlockedBy(DominioDoManaExcellency.class, 2));
        assertEquals(List.of(DominioDoManaExcellency.FOCADO), SkillExcellency.unlockedBy(DominioDoManaExcellency.class, 5));
        assertEquals(List.of(DominioDoManaExcellency.FOCADO, DominioDoManaExcellency.PRODIGIO, DominioDoManaExcellency.LENDA),
                SkillExcellency.unlockedBy(DominioDoManaExcellency.class, 10));
    }
}
