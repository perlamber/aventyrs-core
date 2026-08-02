package org.aventyrs.core.skill;

import org.aventyrs.core.modifier.ModifierResolver;
import org.aventyrs.core.modifier.ModifierResolverImpl;
import org.aventyrs.core.modifier.ModifierType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AtletismoExcellencyTest {

    @Test
    void everyExcellencyBelongsToAtletismo() {
        for (AtletismoExcellency excellency : AtletismoExcellency.values()) {
            assertEquals(SkillType.ATLETISMO, excellency.getSkillType());
        }
    }

    @Test
    void everyExcellencyHasADescription() {
        for (AtletismoExcellency excellency : AtletismoExcellency.values()) {
            assertFalse(excellency.getDescription().isBlank());
        }
    }

    @Test
    void listHasTheThreeUniversalTiers() {
        assertEquals(3, AtletismoExcellency.values().length);
    }

    @Test
    void onlyProdigioReducesDifficulty() {
        for (AtletismoExcellency excellency : AtletismoExcellency.values()) {
            int expected = excellency == AtletismoExcellency.PRODIGIO ? 1 : 0;
            assertEquals(expected, excellency.getDifficultyReduction());
        }
    }

    @Test
    void unlockedByFiltersAtletismoExcellencyByGraduation() {
        assertEquals(List.of(), SkillExcellency.unlockedBy(AtletismoExcellency.class, 2));
        assertEquals(List.of(AtletismoExcellency.FOCADO), SkillExcellency.unlockedBy(AtletismoExcellency.class, 5));
        assertEquals(List.of(AtletismoExcellency.FOCADO, AtletismoExcellency.PRODIGIO, AtletismoExcellency.LENDA),
                SkillExcellency.unlockedBy(AtletismoExcellency.class, 10));
    }

    @Test
    void onlyLendaGrantsAnActionPointsModifier() {
        ModifierResolver modifierResolver = new ModifierResolverImpl();
        for (AtletismoExcellency excellency : AtletismoExcellency.values()) {
            int expected = excellency == AtletismoExcellency.LENDA ? 1 : 0;
            assertEquals(expected, modifierResolver.sumModifiers(excellency, ModifierType.ACTION_POINTS));
        }
    }

    @Test
    void onlyFocadoGrantsAFreeActionsModifier() {
        ModifierResolver modifierResolver = new ModifierResolverImpl();
        for (AtletismoExcellency excellency : AtletismoExcellency.values()) {
            int expected = excellency == AtletismoExcellency.FOCADO ? 1 : 0;
            assertEquals(expected, modifierResolver.sumModifiers(excellency, ModifierType.FREE_ACTIONS));
        }
    }
}
