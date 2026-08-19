package org.aventyrs.core.title.santo;

import org.aventyrs.core.character.services.DamageService;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SantoAbilityTest {

    @Test
    void everyAbilityHasADescription() {
        for (SantoAbility ability : SantoAbility.values()) {
            assertFalse(ability.getDescription().isBlank());
        }
    }

    @Test
    void hasTheFourDescribedAbilities() {
        assertEquals(4, SantoAbility.values().length);
    }

    @Test
    void onlyTheTwoSupremasReportIsSupremeTrue() {
        assertFalse(SantoAbility.PROTECAO_UNGIDA.isSupreme());
        assertFalse(SantoAbility.BASTIAO_DOS_NECESSITADOS.isSupreme());
        assertTrue(SantoAbility.GUARDA_VIDAS.isSupreme());
        assertTrue(SantoAbility.PROTETOR_DA_VIDA_E_DA_MORTE.isSupreme());
    }

    @Test
    void protecaoUngidaHasTheRightActivationCost() {
        assertEquals(3, SantoAbility.PROTECAO_UNGIDA.getPDCost());
        assertEquals(2, SantoAbility.PROTECAO_UNGIDA.getActionPointCost());
        assertFalse(SantoAbility.PROTECAO_UNGIDA.isReactionActivation());
    }

    @Test
    void bastiaoDosNecessitadosHasNoActivationCost() {
        assertEquals(0, SantoAbility.BASTIAO_DOS_NECESSITADOS.getPDCost());
        assertEquals(0, SantoAbility.BASTIAO_DOS_NECESSITADOS.getActionPointCost());
        assertFalse(SantoAbility.BASTIAO_DOS_NECESSITADOS.isReactionActivation());
    }

    @Test
    void guardaVidasHasTheRightActivationCost() {
        assertEquals(2, SantoAbility.GUARDA_VIDAS.getPDCost());
        assertEquals(0, SantoAbility.GUARDA_VIDAS.getActionPointCost());
        assertTrue(SantoAbility.GUARDA_VIDAS.isReactionActivation());
    }

    @Test
    void protetorDaVidaEDaMorteHasNoActivationCost() {
        assertEquals(0, SantoAbility.PROTETOR_DA_VIDA_E_DA_MORTE.getPDCost());
        assertEquals(0, SantoAbility.PROTETOR_DA_VIDA_E_DA_MORTE.getActionPointCost());
        assertFalse(SantoAbility.PROTETOR_DA_VIDA_E_DA_MORTE.isReactionActivation());
    }

    @Test
    void onlyTheTwoNoCostAbilitiesArePassive() {
        assertFalse(SantoAbility.PROTECAO_UNGIDA.isPassive());
        assertTrue(SantoAbility.BASTIAO_DOS_NECESSITADOS.isPassive());
        // Reação, not "Nenhum" — an explicit player trigger, so not passive despite 0 PA.
        assertFalse(SantoAbility.GUARDA_VIDAS.isPassive());
        assertTrue(SantoAbility.PROTETOR_DA_VIDA_E_DA_MORTE.isPassive());
    }

    @Test
    void bastiaoDosNecessitadosGrantsAbsoluteDamageReductionOnlyWhenALowerPvAdjacentAllyExists() {
        assertEquals(DamageService.DEFAULT_DAMAGE_REDUCTION, SantoAbility.BASTIAO_DOS_NECESSITADOS.resolveAbsoluteDamageReduction(null, true));
        assertEquals(0, SantoAbility.BASTIAO_DOS_NECESSITADOS.resolveAbsoluteDamageReduction(null, false));
    }

    @Test
    void everyOtherAbilityGrantsNoAbsoluteDamageReductionRegardlessOfTheCondition() {
        for (SantoAbility ability : SantoAbility.values()) {
            if (ability == SantoAbility.BASTIAO_DOS_NECESSITADOS) {
                continue;
            }
            assertEquals(0, ability.resolveAbsoluteDamageReduction(null, true));
            assertEquals(0, ability.resolveAbsoluteDamageReduction(null, false));
        }
    }

    // No SantoAbility constant has a real, activatable Interaction yet — every Título-level
    // Habilidade/Suprema in this catalog is still fully TODO'd (see each constant's own
    // comment), so getInteractionClass() has nothing real to point to for any of them today.
    @Test
    void noAbilityReportsAnInteractionClassYet() {
        for (SantoAbility ability : SantoAbility.values()) {
            assertEquals(Optional.empty(), ability.getInteractionClass());
        }
    }
}
