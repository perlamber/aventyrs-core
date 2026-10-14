package org.aventyrs.core.item;

import java.util.EnumSet;
import java.util.Set;

import org.aventyrs.core.modifier.ModifierType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PowerStoneCatalogTest {

    /** The V19 stones with at least one {@code ItemBonus}-typed effect. Everything else is catalog-only. */
    private static final Set<PowerStoneType> WIRED = EnumSet.of(
            PowerStoneType.ADAMANTE_BRUTO,       // base: Vigor +1; prot: RD + RDS
            PowerStoneType.CALCITA_VULCANICA,    // base: Margem Menor +1 + RC
            PowerStoneType.CIANITA_DRACONICA,    // base: Instinto +1
            PowerStoneType.ESFERA_DE_AETHER,     // base: Foco +1
            PowerStoneType.FLUORITA_PRISMATICA,  // base: Carisma +1
            PowerStoneType.HEMATITA_DO_VENDAVAL, // base: Destreza +1, MOVEMENT 3; viol: attack advantage
            PowerStoneType.MITRAL_PURO,          // base: MOVEMENT 1 + attack advantage; prot: RC; viol: Margem
            PowerStoneType.OPALA_PURIFICADORA,   // prot: Profano reduction 3
            PowerStoneType.RELAMPAGO_DOURADO,    // base: MOVEMENT 2; viol: +1PA
            PowerStoneType.RUTILO_SUBTERRANEO,   // base: DEFESAS 2 + Atletismo advantage
            PowerStoneType.SODALITA_GELIDA,      // base: Força +1 + RD
            PowerStoneType.SOMBRA_SOLIDIFICADA); // viol: Dano Base +1 (a hook, not an ItemBonus)

    @Test
    void catalogsEveryEntryOfEachPedraDoPoderList() {
        assertEquals(17, PowerStoneType.values().length);
        assertEquals(4, PowerStoneQuality.values().length);
        assertEquals(5, PowerStoneMasterpiece.values().length);
        assertEquals(7, PowerStoneImprovement.values().length);
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
    void wiredStonesGrantExactlyTheirDocumentedBonus() {
        assertEquals(1, PowerStoneType.ADAMANTE_BRUTO.resolveBonus(ModifierType.VIGOR_BONUS, ItemType.OFFENSIVE));
        assertEquals(2, PowerStoneType.ADAMANTE_BRUTO.resolveBonus(ModifierType.DAMAGE_REDUCTION, ItemType.DEFENSIVE));
        assertEquals(1, PowerStoneType.ADAMANTE_BRUTO.resolveBonus(ModifierType.DAMAGE_TAKEN_REDUCTION, ItemType.DEFENSIVE));
        assertEquals(0, PowerStoneType.ADAMANTE_BRUTO.resolveBonus(ModifierType.DAMAGE_REDUCTION, ItemType.OFFENSIVE));

        assertEquals(3, PowerStoneType.HEMATITA_DO_VENDAVAL.resolveBonus(ModifierType.MOVEMENT, ItemType.DEFENSIVE));
        assertEquals(2, PowerStoneType.HEMATITA_DO_VENDAVAL.resolveBonus(
                ModifierType.ATAQUE_CORPO_A_CORPO_ROLL_BONUS, ItemType.OFFENSIVE));
        assertEquals(0, PowerStoneType.HEMATITA_DO_VENDAVAL.resolveBonus(
                ModifierType.ATAQUE_CORPO_A_CORPO_ROLL_BONUS, ItemType.DEFENSIVE));

        assertEquals(1, PowerStoneType.MITRAL_PURO.resolveBonus(ModifierType.MOVEMENT, ItemType.DEFENSIVE));
        assertEquals(1, PowerStoneType.MITRAL_PURO.resolveBonus(ModifierType.LESSER_CRITICAL_MARGIN, ItemType.OFFENSIVE));
        assertEquals(0, PowerStoneType.MITRAL_PURO.resolveBonus(ModifierType.LESSER_CRITICAL_MARGIN,
                ItemType.OFFENSIVE, false));

        assertEquals(1, PowerStoneType.RELAMPAGO_DOURADO.resolveBonus(ModifierType.ACTION_POINTS, ItemType.OFFENSIVE));
        assertEquals(0, PowerStoneType.RELAMPAGO_DOURADO.resolveBonus(ModifierType.ACTION_POINTS, ItemType.DEFENSIVE));

        assertEquals(2, PowerStoneType.RUTILO_SUBTERRANEO.resolveBonus(ModifierType.DEFESAS, ItemType.DEFENSIVE));
        assertEquals(1, PowerStoneType.SOMBRA_SOLIDIFICADA.resolveDamageBaseIncrease(null, ItemType.OFFENSIVE));
    }

    @Test
    void theViolentHooksNameTheirStones() {
        assertEquals(org.aventyrs.core.effect.CriticalEffectType.INFLAMAR,
                PowerStoneType.CITRINO_INCANDESCENTE.getViolentCriticalEffect());
        assertEquals(org.aventyrs.core.effect.CriticalEffectType.OFERENDA_MALDITA,
                PowerStoneType.TURMALINA_OBSCURA.getViolentCriticalEffect());
        assertTrue(PowerStoneType.HEMATITA_DO_VENDAVAL.createViolentEffectChain(false)
                instanceof org.aventyrs.core.effect.Rugido);
        assertEquals(1, PowerStoneType.CALCITA_VULCANICA.resolveCriticalDamage(ItemType.OFFENSIVE, true).extraDice());
    }
}
