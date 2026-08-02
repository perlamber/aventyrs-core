package org.aventyrs.core.skill;

import org.aventyrs.core.modifier.ModifierResolver;
import org.aventyrs.core.modifier.ModifierResolverImpl;
import org.aventyrs.core.modifier.ModifierType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class EsquivaEApararExcellencyTest {

    @Test
    void everyExcellencyBelongsToEsquivaEAparar() {
        for (EsquivaEApararExcellency excellency : EsquivaEApararExcellency.values()) {
            assertEquals(SkillType.ESQUIVA_E_APARAR, excellency.getSkillType());
        }
    }

    @Test
    void everyExcellencyHasADescription() {
        for (EsquivaEApararExcellency excellency : EsquivaEApararExcellency.values()) {
            assertFalse(excellency.getDescription().isBlank());
        }
    }

    @Test
    void listHasTheThreeUniversalTiers() {
        assertEquals(3, EsquivaEApararExcellency.values().length);
    }

    @Test
    void onlyProdigioReducesDifficulty() {
        for (EsquivaEApararExcellency excellency : EsquivaEApararExcellency.values()) {
            int expected = excellency == EsquivaEApararExcellency.PRODIGIO ? 1 : 0;
            assertEquals(expected, excellency.getDifficultyReduction());
        }
    }

    @Test
    void focadoAndLendaEachGrantASkillRollBonusThatAddsUpToThreeTotal() {
        ModifierResolver modifierResolver = new ModifierResolverImpl();
        assertEquals(1, modifierResolver.sumModifiers(EsquivaEApararExcellency.FOCADO, ModifierType.SKILL_ROLL_BONUS));
        assertEquals(2, modifierResolver.sumModifiers(EsquivaEApararExcellency.LENDA, ModifierType.SKILL_ROLL_BONUS));
        assertEquals(0, modifierResolver.sumModifiers(EsquivaEApararExcellency.PRODIGIO, ModifierType.SKILL_ROLL_BONUS));

        int totalOnceBothUnlocked = modifierResolver.sumModifiers(
                List.of(EsquivaEApararExcellency.FOCADO, EsquivaEApararExcellency.LENDA), ModifierType.SKILL_ROLL_BONUS);
        assertEquals(3, totalOnceBothUnlocked);
    }

    @Test
    void unlockedByFiltersEsquivaEApararExcellencyByGraduation() {
        assertEquals(List.of(), SkillExcellency.unlockedBy(EsquivaEApararExcellency.class, 2));
        assertEquals(List.of(EsquivaEApararExcellency.FOCADO), SkillExcellency.unlockedBy(EsquivaEApararExcellency.class, 5));
        assertEquals(List.of(EsquivaEApararExcellency.FOCADO, EsquivaEApararExcellency.PRODIGIO, EsquivaEApararExcellency.LENDA),
                SkillExcellency.unlockedBy(EsquivaEApararExcellency.class, 10));
    }
}
