package org.aventyrs.core.skill;

import org.aventyrs.core.item.Weapon;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The Margem Crítica Maior — 18 by default (three 6s), widened and pushed back like the Menor. */
class MajorCriticalMarginTest {

    @Test
    void theDefaultMarginIsExactlyThreeSixes() {
        assertEquals(CriticalResult.ACERTO_CRITICO_MAIOR, new SkillRoll(List.of(6, 6, 6)).getCriticalResult());
        assertEquals(CriticalResult.ACERTO_CRITICO_MENOR, new SkillRoll(List.of(6, 6, 5)).getCriticalResult());
    }

    @Test
    void aWidenedMarginMakesSeventeenAMaior() {
        CriticalResult result = new SkillRoll(List.of(6, 6, 5)).getCriticalResult(0,
                Weapon.DEFAULT_LESSER_CRITICAL_MARGIN, 17);

        assertEquals(CriticalResult.ACERTO_CRITICO_MAIOR, result);
    }

    @Test
    void theMaiorOutranksAMenorItOverlaps() {
        CriticalResult result = new SkillRoll(List.of(5, 5, 5)).getCriticalResult(5,
                Weapon.DEFAULT_LESSER_CRITICAL_MARGIN, 15);

        assertEquals(CriticalResult.ACERTO_CRITICO_MAIOR, result);
    }

    @Test
    void noWideningOverturnsAFalhaCritica() {
        assertEquals(CriticalResult.FALHA_CRITICA_MAIOR,
                new SkillRoll(List.of(1, 1, 1)).getCriticalResult(20, 5, 1));
        assertEquals(CriticalResult.FALHA_CRITICA_MENOR,
                new SkillRoll(List.of(1, 1, 2)).getCriticalResult(20, 5, 1));
    }
}
