package org.aventyrs.core.sheet;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConditionTypeTest {

    @Test
    void possessaoForbidsCastingAndActivatingAbilities() {
        assertTrue(ConditionType.POSSESSAO.preventsSpellCasting());
        assertTrue(ConditionType.POSSESSAO.preventsAbilityActivation());
    }

    @Test
    void possessaoTaxesNothingNumericallyAndImpliesNoOtherCondition() {
        // Its rules text is entirely prohibitions plus a targeting compulsion nothing models.
        assertTrue(ConditionType.POSSESSAO.getEffects().isEmpty());
        assertTrue(ConditionType.POSSESSAO.getImplied().isEmpty());
    }

    @Test
    void everyConditionIsAMaleficioExceptEscondidoAndPetrificado() {
        assertFalse(ConditionType.ESCONDIDO.isMaleficio());
        assertFalse(ConditionType.PETRIFICADO.isMaleficio());
        // Rendido (core 0.1.6.19) is a voluntary surrender — nothing imposes it.
        assertFalse(ConditionType.RENDIDO.isMaleficio());
        Arrays.stream(ConditionType.values())
                .filter(type -> type != ConditionType.ESCONDIDO && type != ConditionType.PETRIFICADO
                        && type != ConditionType.RENDIDO)
                .forEach(type -> assertTrue(type.isMaleficio(), type + " should be a Malefício"));
    }

    @Test
    void maleficiosIsEveryConstantButEscondidoAndPetrificado() {
        assertEquals(ConditionType.values().length - 3, ConditionType.maleficios().size());
        assertFalse(ConditionType.maleficios().contains(ConditionType.ESCONDIDO));
        assertFalse(ConditionType.maleficios().contains(ConditionType.PETRIFICADO));
        assertTrue(ConditionType.maleficios().contains(ConditionType.POSSESSAO));
    }
}
