package org.aventyrs.core.feat;

import org.aventyrs.core.item.AttackMethod;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The choice-carrying instance itself: what it holds, and that it still reads as its catalog
 * constant. What the choice is <em>worth</em> is asserted where it lands — through the Interaction
 * in {@code GeneralFeatEffectIntegrationTest}, and through a delivered attack in {@code
 * ChoiceFeatAttackDeliveryTest}.
 */
class EspecialistaEmArmaFeatTest {

    @Test
    void delegatesIdentityToTheCatalogConstant() {
        EspecialistaEmArmaFeat feat = EspecialistaEmArmaFeat.of(AttackMethod.BOW);

        assertSame(DuelistaFeat.ESPECIALISTA_EM_ARMA, feat.catalogEntry());
        assertEquals(DuelistaFeat.ESPECIALISTA_EM_ARMA.getDescription(), feat.getDescription());
    }

    @Test
    void requiresAChosenMethod() {
        assertThrows(NullPointerException.class, () -> EspecialistaEmArmaFeat.of(null));
    }

    @Test
    void carriesTheChosenMethod() {
        assertEquals(AttackMethod.LIGHT_BLADE,
                EspecialistaEmArmaFeat.of(AttackMethod.LIGHT_BLADE).getChosenMethod());
    }
}
