package org.aventyrs.core.ego;

import org.aventyrs.core.character.EgoDomain;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AutocontroleAdvantageTest {

    @Test
    void everyAdvantageBelongsToAutocontrole() {
        for (AutocontroleAdvantage advantage : AutocontroleAdvantage.values()) {
            assertEquals(EgoDomain.AUTOCONTROLE, advantage.getEgoDomain());
        }
    }

    @Test
    void everyAdvantageHasADescription() {
        for (AutocontroleAdvantage advantage : AutocontroleAdvantage.values()) {
            assertFalse(advantage.getDescription().isBlank());
        }
    }

    @Test
    void listHasTheThreeDescribedAdvantages() {
        assertEquals(3, AutocontroleAdvantage.values().length);
    }

    @Test
    void MOTIVACAO_DE_MOSESRecoversOneExtraTemporaryAutocontrolePointPerSession() {
        assertEquals(1, AutocontroleAdvantage.MOTIVACAO_DE_MOSES.resolveExtraSessionEgoRecovery());
    }

    @Test
    void noOtherAdvantageRecoversAnExtraSessionEgoPoint() {
        for (AutocontroleAdvantage advantage : AutocontroleAdvantage.values()) {
            if (advantage != AutocontroleAdvantage.MOTIVACAO_DE_MOSES) {
                assertEquals(0, advantage.resolveExtraSessionEgoRecovery());
            }
        }
    }
}
