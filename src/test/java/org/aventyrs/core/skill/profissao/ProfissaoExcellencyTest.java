package org.aventyrs.core.skill.profissao;

import org.aventyrs.core.skill.SkillExcellency;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ProfissaoExcellencyTest {

    @Test
    void everyExcellencyBelongsToProfissao() {
        for (ProfissaoExcellency excellency : ProfissaoExcellency.values()) {
            assertEquals(SkillType.PROFISSAO, excellency.getSkillType());
        }
    }

    @Test
    void everyExcellencyHasADescription() {
        for (ProfissaoExcellency excellency : ProfissaoExcellency.values()) {
            assertFalse(excellency.getDescription().isBlank());
        }
    }

    @Test
    void listHasTheThreeUniversalTiers() {
        assertEquals(3, ProfissaoExcellency.values().length);
    }

    @Test
    void onlyProdigioReducesDifficulty() {
        for (ProfissaoExcellency excellency : ProfissaoExcellency.values()) {
            int expected = excellency == ProfissaoExcellency.PRODIGIO ? 1 : 0;
            assertEquals(expected, excellency.getDifficultyReduction());
        }
    }

    @Test
    void unlockedByFiltersProfissaoExcellencyByGraduation() {
        assertEquals(List.of(), SkillExcellency.unlockedBy(ProfissaoExcellency.class, 2));
        assertEquals(List.of(ProfissaoExcellency.FOCADO), SkillExcellency.unlockedBy(ProfissaoExcellency.class, 5));
        assertEquals(List.of(ProfissaoExcellency.FOCADO, ProfissaoExcellency.PRODIGIO, ProfissaoExcellency.LENDA),
                SkillExcellency.unlockedBy(ProfissaoExcellency.class, 10));
    }
}
