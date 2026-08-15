package org.aventyrs.core.ability;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.skill.CriticalResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CharismaAbilityTest {

    @Test
    void everyAbilityBelongsToCharisma() {
        for (CharismaAbility ability : CharismaAbility.values()) {
            assertEquals(AttributeDomain.CHARISMA, ability.getAttributeDomain());
        }
    }

    @Test
    void everyAbilityHasADescription() {
        for (CharismaAbility ability : CharismaAbility.values()) {
            assertFalse(ability.getDescription().isBlank());
        }
    }

    @Test
    void listHasTheFiveDescribedAbilities() {
        assertEquals(5, CharismaAbility.values().length);
    }

    @Test
    void destinoFavoravelGrantsSorteAndAutocontroleOnMajorCriticalSuccess() {
        assertEquals(List.of(EgoDomain.SORTE, EgoDomain.AUTOCONTROLE),
                CharismaAbility.DESTINO_FAVORAVEL.resolveCriticalSuccessEgoGain(CriticalResult.ACERTO_CRITICO_MAIOR));
    }

    @Test
    void destinoFavoravelGrantsNothingOnAnyOtherCriticalResult() {
        for (CriticalResult criticalResult : CriticalResult.values()) {
            if (criticalResult == CriticalResult.ACERTO_CRITICO_MAIOR) {
                continue;
            }
            assertTrue(CharismaAbility.DESTINO_FAVORAVEL.resolveCriticalSuccessEgoGain(criticalResult).isEmpty());
        }
    }

    @Test
    void noOtherAbilityGrantsAnEgoGainOnMajorCriticalSuccess() {
        for (CharismaAbility ability : CharismaAbility.values()) {
            if (ability == CharismaAbility.DESTINO_FAVORAVEL) {
                continue;
            }
            assertTrue(ability.resolveCriticalSuccessEgoGain(CriticalResult.ACERTO_CRITICO_MAIOR).isEmpty());
        }
    }
}
