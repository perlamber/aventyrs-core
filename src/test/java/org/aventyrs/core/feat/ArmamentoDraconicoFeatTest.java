package org.aventyrs.core.feat;

import org.aventyrs.core.item.NaturalWeapon;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArmamentoDraconicoFeatTest {

    @Test
    void delegatesIdentityToTheCatalogConstant() {
        ArmamentoDraconicoFeat feat = ArmamentoDraconicoFeat.of(
                NaturalWeapon.CHIFRES_PODEROSOS, NaturalWeapon.PRESAS_LONGAS);

        assertSame(DraconicoFeat.ARMAMENTO_DRACONICO, feat.catalogEntry());
        assertEquals(DraconicoFeat.ARMAMENTO_DRACONICO.getDescription(), feat.getDescription());
        assertEquals(DraconicoFeat.ARMAMENTO_DRACONICO.getFeatRequirements(), feat.getFeatRequirements());
    }

    @Test
    void grantsExactlyTheTwoChosenArmasNaturais() {
        ArmamentoDraconicoFeat feat = ArmamentoDraconicoFeat.of(
                NaturalWeapon.GARRAS_AFIADAS, NaturalWeapon.CAUDA_CHICOTE);

        assertEquals(EnumSet.of(NaturalWeapon.GARRAS_AFIADAS, NaturalWeapon.CAUDA_CHICOTE),
                EnumSet.copyOf(feat.getGrantedNaturalWeapons(null)));
    }

    @Test
    void aHolderSatisfiesARequiredFeatGateThroughTheCatalogEntry() {
        ArmamentoDraconicoFeat feat = ArmamentoDraconicoFeat.of(
                NaturalWeapon.CHIFRES_PODEROSOS, NaturalWeapon.CAUDA_CHICOTE);

        assertSame(DraconicoFeat.ARMAMENTO_DRACONICO, feat.catalogEntry());
        assertTrue(ArmamentoDraconicoFeat.ALLOWED_CHOICES.containsAll(feat.getChosenWeapons()));
    }

    @Test
    void rejectsFewerThanTwoChoices() {
        assertThrows(IllegalArgumentException.class,
                () -> new ArmamentoDraconicoFeat(EnumSet.of(NaturalWeapon.CHIFRES_PODEROSOS)));
    }

    @Test
    void rejectsMoreThanTwoChoices() {
        assertThrows(IllegalArgumentException.class, () -> new ArmamentoDraconicoFeat(EnumSet.of(
                NaturalWeapon.CHIFRES_PODEROSOS, NaturalWeapon.PRESAS_LONGAS, NaturalWeapon.GARRAS_AFIADAS)));
    }

    @Test
    void rejectsAWeaponOutsideTheAllowedFour() {
        assertThrows(IllegalArgumentException.class, () -> ArmamentoDraconicoFeat.of(
                NaturalWeapon.CHIFRES_PODEROSOS, NaturalWeapon.ARMA_DE_SOPRO));
    }

    @Test
    void rejectsTheSameWeaponPickedTwice() {
        assertThrows(IllegalArgumentException.class, () -> ArmamentoDraconicoFeat.of(
                NaturalWeapon.GARRAS_AFIADAS, NaturalWeapon.GARRAS_AFIADAS));
    }

    @Test
    void requiresANonNullChoiceSet() {
        assertThrows(NullPointerException.class, () -> new ArmamentoDraconicoFeat(null));
    }
}
