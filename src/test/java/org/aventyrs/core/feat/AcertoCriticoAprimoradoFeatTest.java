package org.aventyrs.core.feat;

import org.aventyrs.core.item.AttackMethod;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The choice-carrying instance itself. The widened Margem Crítica it buys is asserted where it
 * lands — through a delivered attack in {@code ChoiceFeatAttackDeliveryTest} for the weapon branch,
 * and through the Interaction in {@code GeneralFeatEffectIntegrationTest} for the Magia branch.
 */
class AcertoCriticoAprimoradoFeatTest {

    @Test
    void delegatesIdentityToTheCatalogConstant() {
        AcertoCriticoAprimoradoFeat feat = AcertoCriticoAprimoradoFeat.of(AttackMethod.OFFENSIVE_MAGIC);

        assertSame(AssassinoFeat.ACERTO_CRITICO_APRIMORADO, feat.catalogEntry());
    }

    @Test
    void requiresAChosenMethod() {
        assertThrows(NullPointerException.class, () -> AcertoCriticoAprimoradoFeat.of(null));
    }

    @Test
    void carriesTheChosenMethod() {
        assertEquals(AttackMethod.OFFENSIVE_MAGIC,
                AcertoCriticoAprimoradoFeat.of(AttackMethod.OFFENSIVE_MAGIC).getChosenMethod());
    }
}
