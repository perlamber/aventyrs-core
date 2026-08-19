package org.aventyrs.core.title.santo;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbencoadoPelaLuzAbilityTest {

    @Test
    void everyAbilityHasADescription() {
        for (AbencoadoPelaLuzAbility ability : AbencoadoPelaLuzAbility.values()) {
            assertFalse(ability.getDescription().isBlank());
        }
    }

    @Test
    void hasTheFourDescribedAbilities() {
        assertEquals(4, AbencoadoPelaLuzAbility.values().length);
    }

    @Test
    void onlyGloriaRelampejanteDeTeslaReportsIsSupremeTrue() {
        assertFalse(AbencoadoPelaLuzAbility.ORGULHO_ELDURIANO.isSupreme());
        assertFalse(AbencoadoPelaLuzAbility.GRITO_DE_GUERRA_VULCANO.isSupreme());
        assertFalse(AbencoadoPelaLuzAbility.PELE_ROCHOSA_DE_EPONA.isSupreme());
        assertTrue(AbencoadoPelaLuzAbility.GLORIA_RELAMPEJANTE_DE_TESLA.isSupreme());
    }

    @Test
    void orgulhoEldurianoReportsItsMinimumVariableCost() {
        assertEquals(1, AbencoadoPelaLuzAbility.ORGULHO_ELDURIANO.getPDCost());
        assertEquals(2, AbencoadoPelaLuzAbility.ORGULHO_ELDURIANO.getActionPointCost());
        assertFalse(AbencoadoPelaLuzAbility.ORGULHO_ELDURIANO.isReactionActivation());
        assertFalse(AbencoadoPelaLuzAbility.ORGULHO_ELDURIANO.isFreeActionActivation());
    }

    @Test
    void gritoDeGuerraVulcanoHasTheRightActivationCost() {
        assertEquals(3, AbencoadoPelaLuzAbility.GRITO_DE_GUERRA_VULCANO.getPDCost());
        assertEquals(1, AbencoadoPelaLuzAbility.GRITO_DE_GUERRA_VULCANO.getActionPointCost());
        assertFalse(AbencoadoPelaLuzAbility.GRITO_DE_GUERRA_VULCANO.isReactionActivation());
        assertFalse(AbencoadoPelaLuzAbility.GRITO_DE_GUERRA_VULCANO.isFreeActionActivation());
    }

    @Test
    void peleRochosaDeEponaHasTheRightActivationCost() {
        assertEquals(2, AbencoadoPelaLuzAbility.PELE_ROCHOSA_DE_EPONA.getPDCost());
        assertEquals(2, AbencoadoPelaLuzAbility.PELE_ROCHOSA_DE_EPONA.getActionPointCost());
        assertFalse(AbencoadoPelaLuzAbility.PELE_ROCHOSA_DE_EPONA.isReactionActivation());
        assertFalse(AbencoadoPelaLuzAbility.PELE_ROCHOSA_DE_EPONA.isFreeActionActivation());
    }

    @Test
    void gloriaRelampejanteDeTeslaIsAFreeActionActivation() {
        assertEquals(2, AbencoadoPelaLuzAbility.GLORIA_RELAMPEJANTE_DE_TESLA.getPDCost());
        assertEquals(0, AbencoadoPelaLuzAbility.GLORIA_RELAMPEJANTE_DE_TESLA.getActionPointCost());
        assertFalse(AbencoadoPelaLuzAbility.GLORIA_RELAMPEJANTE_DE_TESLA.isReactionActivation());
        assertTrue(AbencoadoPelaLuzAbility.GLORIA_RELAMPEJANTE_DE_TESLA.isFreeActionActivation());
    }

    @Test
    void noAbilityIsPassive() {
        // All four have an explicit activation (PA, or Ação Livre for GLORIA_RELAMPEJANTE_DE_TESLA
        // despite its 0 actionPointCost) — none is "Custo de Ativação: Nenhum, habilidade passiva".
        for (AbencoadoPelaLuzAbility ability : AbencoadoPelaLuzAbility.values()) {
            assertFalse(ability.isPassive());
        }
    }

    @Test
    void noAbilityGrantsAbsoluteDamageReductionYet() {
        for (AbencoadoPelaLuzAbility ability : AbencoadoPelaLuzAbility.values()) {
            assertEquals(0, ability.resolveAbsoluteDamageReduction(null, true));
        }
    }

    // Only GRITO_DE_GUERRA_VULCANO has a real, activatable Interaction — its own Vantagem/
    // Defesas Blessings are reported by GritoDeGuerraVulcanoInteraction (see Santo
    // #activateGritoDeGuerraVulcano). The other three constants are still fully TODO'd, so
    // getInteractionClass() has nothing real to point to for them yet.
    @Test
    void onlyGritoDeGuerraVulcanoReportsAnInteractionClass() {
        assertEquals(Optional.empty(), AbencoadoPelaLuzAbility.ORGULHO_ELDURIANO.getInteractionClass());
        assertEquals(Optional.of(GritoDeGuerraVulcanoInteraction.class), AbencoadoPelaLuzAbility.GRITO_DE_GUERRA_VULCANO.getInteractionClass());
        assertEquals(Optional.empty(), AbencoadoPelaLuzAbility.PELE_ROCHOSA_DE_EPONA.getInteractionClass());
        assertEquals(Optional.empty(), AbencoadoPelaLuzAbility.GLORIA_RELAMPEJANTE_DE_TESLA.getInteractionClass());
    }
}
