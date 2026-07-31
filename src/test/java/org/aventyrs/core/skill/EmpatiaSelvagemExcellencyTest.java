package org.aventyrs.core.skill;

import org.aventyrs.core.modifier.ModifierResolver;
import org.aventyrs.core.modifier.ModifierResolverImpl;
import org.aventyrs.core.modifier.ModifierType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class EmpatiaSelvagemExcellencyTest {

    @Test
    void everyExcellencyBelongsToEmpatiaSelvagem() {
        for (EmpatiaSelvagemExcellency excellency : EmpatiaSelvagemExcellency.values()) {
            assertEquals(SkillType.EMPATIA_SELVAGEM, excellency.getSkillType());
        }
    }

    @Test
    void everyExcellencyHasADescription() {
        for (EmpatiaSelvagemExcellency excellency : EmpatiaSelvagemExcellency.values()) {
            assertFalse(excellency.getDescription().isBlank());
        }
    }

    @Test
    void listHasTheThreeUniversalTiers() {
        assertEquals(3, EmpatiaSelvagemExcellency.values().length);
    }

    @Test
    void onlyProdigioReducesDifficulty() {
        for (EmpatiaSelvagemExcellency excellency : EmpatiaSelvagemExcellency.values()) {
            int expected = excellency == EmpatiaSelvagemExcellency.PRODIGIO ? 1 : 0;
            assertEquals(expected, excellency.getDifficultyReduction());
        }
    }

    @Test
    void onlyFocadoGrantsASkillRollBonus() {
        ModifierResolver modifierResolver = new ModifierResolverImpl();
        for (EmpatiaSelvagemExcellency excellency : EmpatiaSelvagemExcellency.values()) {
            int expected = excellency == EmpatiaSelvagemExcellency.FOCADO ? Skill.ADVANTAGE_BONUS : 0;
            assertEquals(expected, modifierResolver.sumModifiers(excellency, ModifierType.SKILL_ROLL_BONUS));
        }
    }

    @Test
    void unlockedByFiltersEmpatiaSelvagemExcellencyByGraduation() {
        assertEquals(List.of(), SkillExcellency.unlockedBy(EmpatiaSelvagemExcellency.class, 2));
        assertEquals(List.of(EmpatiaSelvagemExcellency.FOCADO), SkillExcellency.unlockedBy(EmpatiaSelvagemExcellency.class, 5));
        assertEquals(List.of(EmpatiaSelvagemExcellency.FOCADO, EmpatiaSelvagemExcellency.PRODIGIO, EmpatiaSelvagemExcellency.LENDA),
                SkillExcellency.unlockedBy(EmpatiaSelvagemExcellency.class, 10));
    }
}
