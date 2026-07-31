package org.aventyrs.core.skill;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class MedicinaECuraExcellencyTest {

    @Test
    void everyExcellencyBelongsToMedicinaECura() {
        for (MedicinaECuraExcellency excellency : MedicinaECuraExcellency.values()) {
            assertEquals(SkillType.MEDICINA_E_CURA, excellency.getSkillType());
        }
    }

    @Test
    void everyExcellencyHasADescription() {
        for (MedicinaECuraExcellency excellency : MedicinaECuraExcellency.values()) {
            assertFalse(excellency.getDescription().isBlank());
        }
    }

    @Test
    void listHasTheThreeUniversalTiers() {
        assertEquals(3, MedicinaECuraExcellency.values().length);
    }

    @Test
    void onlyProdigioReducesDifficulty() {
        for (MedicinaECuraExcellency excellency : MedicinaECuraExcellency.values()) {
            int expected = excellency == MedicinaECuraExcellency.PRODIGIO ? 1 : 0;
            assertEquals(expected, excellency.getDifficultyReduction());
        }
    }

    @Test
    void unlockedByFiltersMedicinaECuraExcellencyByGraduation() {
        assertEquals(List.of(), SkillExcellency.unlockedBy(MedicinaECuraExcellency.class, 2));
        assertEquals(List.of(MedicinaECuraExcellency.FOCADO), SkillExcellency.unlockedBy(MedicinaECuraExcellency.class, 5));
        assertEquals(List.of(MedicinaECuraExcellency.FOCADO, MedicinaECuraExcellency.PRODIGIO, MedicinaECuraExcellency.LENDA),
                SkillExcellency.unlockedBy(MedicinaECuraExcellency.class, 10));
    }
}
