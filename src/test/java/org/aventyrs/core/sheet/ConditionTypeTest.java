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
    void everyConditionIsAMaleficioExceptEscondido() {
        assertFalse(ConditionType.ESCONDIDO.isMaleficio());
        Arrays.stream(ConditionType.values())
                .filter(type -> type != ConditionType.ESCONDIDO)
                .forEach(type -> assertTrue(type.isMaleficio(), type + " should be a Malefício"));
    }

    @Test
    void maleficiosIsEveryConstantButEscondido() {
        assertEquals(ConditionType.values().length - 1, ConditionType.maleficios().size());
        assertFalse(ConditionType.maleficios().contains(ConditionType.ESCONDIDO));
        assertTrue(ConditionType.maleficios().contains(ConditionType.POSSESSAO));
    }
}
