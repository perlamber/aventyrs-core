package org.aventyrs.core.item;

import java.util.EnumSet;
import java.util.Set;

import org.aventyrs.core.modifier.ModifierType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PowerStoneCatalogTest {

    /** The stones with at least one mechanically-expressible mode effect. Everything else is catalog-only. */
    private static final Set<PowerStoneType> WIRED = EnumSet.of(
            PowerStoneType.ADAMANTE_BRUTO,       // def: RD 1
            PowerStoneType.CALCITA_VULCANICA,    // base: RD 1
            PowerStoneType.HEMATITA_DO_VENDAVAL, // base: MOVEMENT 2; off: attack advantage
            PowerStoneType.MITRAL_PURO,          // base: MOVEMENT 2 + attack advantage
            PowerStoneType.RELAMPAGO_DOURADO,    // base: MOVEMENT 2
            PowerStoneType.RUTILO_SUBTERRANEO,   // base: DEFESAS 2 + Atletismo advantage
            PowerStoneType.SOMBRA_SOLIDIFICADA); // def: RD 1; off: Dano Base +1

    @Test
    void catalogsEveryEntryOfEachPedraDoPoderList() {
        assertEquals(17, PowerStoneType.values().length);
        assertEquals(4, PowerStoneQuality.values().length);
        assertEquals(5, PowerStoneMasterpiece.values().length);
        assertEquals(1, PowerStoneImprovement.values().length);
    }

    @Test
    void everyTypeCarriesAllThreeModeEffectTextsAndAnOriginLabel() {
        for (PowerStoneType type : PowerStoneType.values()) {
            assertFalse(type.getName().isBlank(), type + " name");
            assertFalse(type.getOriginLabel().isBlank(), type + " origin");
            assertFalse(type.getBaseEffect().isBlank(), type + " base effect");
            assertFalse(type.getDefensiveEffect().isBlank(), type + " defensive effect");
            assertFalse(type.getOffensiveEffect().isBlank(), type + " offensive effect");
        }
    }

    @Test
    void everyQualityCarriesItsFiveNumericColumns() {
        for (PowerStoneQuality quality : PowerStoneQuality.values()) {
            assertTrue(quality.getPrice() > 0, quality + " price");
            assertTrue(quality.getCharges() > 0, quality + " charges");
            assertTrue(quality.getBindingDamage() > 0, quality + " binding damage");
            assertTrue(quality.getCooldownRounds() > 0, quality + " cooldown");
            assertTrue(quality.getEffectDurationRounds() > 0, quality + " effect duration");
        }
        assertFalse(PowerStoneQuality.JOLDA.isMasterpieceAllowed());
        assertTrue(PowerStoneQuality.JOIA.isMasterpieceAllowed());
        assertTrue(PowerStoneQuality.RELIQUIA.isMasterpieceAllowed());
        assertTrue(PowerStoneQuality.AETHERNUM.isMasterpieceAllowed());
    }

    @Test
    void everyCatalogOnlyStoneGrantsNothingMechanicalInAnyMode() {
        for (PowerStoneType type : PowerStoneType.values()) {
            if (WIRED.contains(type)) {
                continue;
            }
            for (ModifierType modifierType : ModifierType.values()) {
                for (ItemType hostType : ItemType.values()) {
                    assertEquals(0, type.resolveBonus(modifierType, hostType),
                            type + " should grant nothing for " + modifierType + " on a " + hostType + " host");
                }
            }
            assertEquals(0, type.resolveDamageBaseIncrease(null, ItemType.OFFENSIVE), type + " Dano Base");
        }
    }

    @Test
    void wiredStonesGrantExactlyTheirDocumentedBonusAndNothingElse() {
        assertEquals(1, PowerStoneType.ADAMANTE_BRUTO.resolveBonus(ModifierType.DAMAGE_REDUCTION, ItemType.DEFENSIVE));
        assertEquals(0, PowerStoneType.ADAMANTE_BRUTO.resolveBonus(ModifierType.DAMAGE_REDUCTION, ItemType.OFFENSIVE));

        assertEquals(1, PowerStoneType.CALCITA_VULCANICA.resolveBonus(ModifierType.DAMAGE_REDUCTION, ItemType.OFFENSIVE));

        assertEquals(2, PowerStoneType.HEMATITA_DO_VENDAVAL.resolveBonus(ModifierType.MOVEMENT, ItemType.DEFENSIVE));
        assertEquals(2, PowerStoneType.HEMATITA_DO_VENDAVAL.resolveBonus(
                ModifierType.ATAQUE_CORPO_A_CORPO_ROLL_BONUS, ItemType.OFFENSIVE));
        assertEquals(0, PowerStoneType.HEMATITA_DO_VENDAVAL.resolveBonus(
                ModifierType.ATAQUE_CORPO_A_CORPO_ROLL_BONUS, ItemType.DEFENSIVE));

        assertEquals(2, PowerStoneType.MITRAL_PURO.resolveBonus(ModifierType.MOVEMENT, ItemType.DEFENSIVE));
        assertEquals(2, PowerStoneType.MITRAL_PURO.resolveBonus(
                ModifierType.ATAQUE_A_DISTANCIA_ROLL_BONUS, ItemType.DEFENSIVE));

        assertEquals(2, PowerStoneType.RELAMPAGO_DOURADO.resolveBonus(ModifierType.MOVEMENT, ItemType.OFFENSIVE));

        assertEquals(2, PowerStoneType.RUTILO_SUBTERRANEO.resolveBonus(ModifierType.DEFESAS, ItemType.DEFENSIVE));
        assertEquals(2, PowerStoneType.RUTILO_SUBTERRANEO.resolveBonus(
                ModifierType.ATLETISMO_ROLL_BONUS, ItemType.DEFENSIVE));

        assertEquals(1, PowerStoneType.SOMBRA_SOLIDIFICADA.resolveBonus(ModifierType.DAMAGE_REDUCTION, ItemType.DEFENSIVE));
        assertEquals(1, PowerStoneType.SOMBRA_SOLIDIFICADA.resolveDamageBaseIncrease(null, ItemType.OFFENSIVE));
    }
}
