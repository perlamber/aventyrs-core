package org.aventyrs.core.magic;

import org.aventyrs.core.effect.CriticalEffectType;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpellAlternateEffectTest {

    @Test
    void anUnsetColumnIsInheritedRatherThanBlank() {
        SpellAlternateEffect alternate = SpellAlternateEffect.named("Cura em Massa");

        assertEquals("Cura em Massa", alternate.name());
        assertNull(alternate.activationTime());
        assertNull(alternate.targeting());
        assertNull(alternate.castingDifficultyFlooredByTargetMagicDefense());
        assertFalse(alternate.suppressesCriticalEffect());
        assertTrue(alternate.cleansedConditions().isEmpty());
    }

    @Test
    void suppressingACriticalEffectIsDistinctFromLeavingItInherited() {
        SpellAlternateEffect inherited = SpellAlternateEffect.named("Cura em Massa");
        SpellAlternateEffect suppressed = SpellAlternateEffect.builder()
                .name("Portal de Fuga")
                .suppressesCriticalEffect(true)
                .build();

        assertNull(inherited.criticalEffectType());
        assertFalse(inherited.suppressesCriticalEffect());
        assertNull(suppressed.criticalEffectType());
        assertTrue(suppressed.suppressesCriticalEffect());
    }

    @Test
    void namingACriticalEffectWhileSuppressingEveryOneIsAnAuthoringMistake() {
        assertThrows(IllegalOperationException.class, () -> SpellAlternateEffect.builder()
                .name("contradictory")
                .criticalEffectType(CriticalEffectType.INFLAMAR)
                .suppressesCriticalEffect(true)
                .build());
    }

    @Test
    void theDmFloorIsTriStateSoItCanBeSwitchedOffAsWellAsOn() {
        SpellAlternateEffect off = SpellAlternateEffect.builder()
                .name("Benção Bifurcada")
                .castingDifficultyFlooredByTargetMagicDefense(false)
                .build();

        assertEquals(Boolean.FALSE, off.castingDifficultyFlooredByTargetMagicDefense());
        assertNull(SpellAlternateEffect.named("x").castingDifficultyFlooredByTargetMagicDefense());
    }

    @Test
    void anAlternateMustBeNamed() {
        assertThrows(IllegalOperationException.class, () -> SpellAlternateEffect.named("  "));
        assertThrows(IllegalOperationException.class, () -> SpellAlternateEffect.named(null));
    }

    @Test
    void aNegativeManaCostIsRejectedButZeroIsAValidWaiver() {
        assertThrows(IllegalOperationException.class,
                () -> SpellAlternateEffect.builder().name("x").manaCost(-1).build());
        // "dispensa o uso de PM" — Transferir o Dom
        assertEquals(0, SpellAlternateEffect.builder().name("Transferir o Dom").manaCost(0).build().manaCost());
    }
}
