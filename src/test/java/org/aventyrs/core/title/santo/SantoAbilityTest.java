package org.aventyrs.core.title.santo;

import org.aventyrs.core.character.services.DamageService;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.title.PDCost;
import org.aventyrs.core.title.AventyrTitle;
import org.junit.jupiter.api.Test;

import java.util.List;
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
        assertEquals(PDCost.fixed(3), SantoAbility.PROTECAO_UNGIDA.getPDCost());
        assertEquals(ActionCost.ofActionPoints(2), SantoAbility.PROTECAO_UNGIDA.getActionPointCost());
    }

    @Test
    void bastiaoDosNecessitadosHasNoActivationCost() {
        assertEquals(PDCost.fixed(0), SantoAbility.BASTIAO_DOS_NECESSITADOS.getPDCost());
        assertEquals(ActionCost.NONE, SantoAbility.BASTIAO_DOS_NECESSITADOS.getActionPointCost());
    }

    @Test
    void guardaVidasHasTheRightActivationCost() {
        assertEquals(PDCost.fixed(2), SantoAbility.GUARDA_VIDAS.getPDCost());
        assertEquals(ActionCost.REACTION, SantoAbility.GUARDA_VIDAS.getActionPointCost());
    }

    @Test
    void protetorDaVidaEDaMorteHasNoActivationCost() {
        assertEquals(PDCost.fixed(0), SantoAbility.PROTETOR_DA_VIDA_E_DA_MORTE.getPDCost());
        assertEquals(ActionCost.NONE, SantoAbility.PROTETOR_DA_VIDA_E_DA_MORTE.getActionPointCost());
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

    @Test
    void everyAbilityRequiresOneEspecializacao() {
        for (SantoAbility ability : SantoAbility.values()) {
            assertEquals(1, ability.getRequiredSpecializations());
        }
    }

    @Test
    void eachAbilityRequiresItsOwnNumberOfOtherAbilities() {
        assertEquals(0, SantoAbility.PROTECAO_UNGIDA.getRequiredOtherAbilities());
        assertEquals(2, SantoAbility.BASTIAO_DOS_NECESSITADOS.getRequiredOtherAbilities());
        assertEquals(2, SantoAbility.GUARDA_VIDAS.getRequiredOtherAbilities());
        assertEquals(4, SantoAbility.PROTETOR_DA_VIDA_E_DA_MORTE.getRequiredOtherAbilities());
    }

    @Test
    void isEligibleRejectsATitleWithNoEspecializacao() {
        AventyrTitle title = new Santo(List.of(), List.of());

        for (SantoAbility ability : SantoAbility.values()) {
            assertFalse(ability.isEligible(title));
        }
    }

    @Test
    void isEligibleAcceptsProtecaoUngidaOnceOneEspecializacaoIsHeld() {
        AventyrTitle title = new Santo(List.of(SantoSpecialization.ABENCOADO_PELA_LUZ), List.of());

        assertTrue(SantoAbility.PROTECAO_UNGIDA.isEligible(title));
    }

    @Test
    void isEligibleRejectsBastiaoDosNecessitadosWithoutEnoughOtherAbilities() {
        AventyrTitle title = new Santo(List.of(SantoSpecialization.ABENCOADO_PELA_LUZ), List.of(SantoAbility.PROTECAO_UNGIDA));

        assertFalse(SantoAbility.BASTIAO_DOS_NECESSITADOS.isEligible(title));
    }

    @Test
    void isEligibleAcceptsBastiaoDosNecessitadosOnceEnoughOtherAbilitiesAreHeld() {
        AventyrTitle title = new Santo(
                List.of(SantoSpecialization.ABENCOADO_PELA_LUZ),
                List.of(SantoAbility.PROTECAO_UNGIDA, SantoAbility.GUARDA_VIDAS));

        assertTrue(SantoAbility.BASTIAO_DOS_NECESSITADOS.isEligible(title));
    }

    /**
     * "Requer 1 Especialização e <b>2 outras Habilidades de Santo</b>" names the *Título*, so
     * every Habilidade the Santo holds counts — including ones an Especialização brought. They
     * are Habilidades de Santo too; that is what holding the Especialização means.
     *
     * <p>Regression: these were counted per Java enum, so a Santo whose two Habilidades both came
     * from an Especialização counted 0 and could never acquire Bastião dos Necessitados or
     * Guarda-Vidas at all — the two were simply missing from everything that offers acquirable
     * abilities, with nothing to say why.
     */
    @Test
    void isEligibleCountsEspecializacaoGatedHabilidadesTowardOutrasHabilidadesDeSanto() {
        AventyrTitle title = new Santo(
                List.of(SantoSpecialization.ABENCOADO_PELA_LUZ),
                List.of(AbencoadoPelaLuzAbility.ORGULHO_ELDURIANO,
                        AbencoadoPelaLuzAbility.PELE_ROCHOSA_DE_EPONA));

        assertTrue(SantoAbility.BASTIAO_DOS_NECESSITADOS.isEligible(title));
        assertTrue(SantoAbility.GUARDA_VIDAS.isEligible(title));
    }

    /** The two sources are interchangeable, not merely additive-when-same-kind: one Habilidade de
     * Santo plus one an Especialização brought is still two Habilidades de Santo. */
    @Test
    void isEligibleMixesTitleLevelAndEspecializacaoGatedHabilidadesInTheSameCount() {
        AventyrTitle title = new Santo(
                List.of(SantoSpecialization.ABENCOADO_PELA_LUZ),
                List.of(SantoAbility.PROTECAO_UNGIDA, AbencoadoPelaLuzAbility.ORGULHO_ELDURIANO));

        assertTrue(SantoAbility.BASTIAO_DOS_NECESSITADOS.isEligible(title));
    }

    /** "Outras" excludes the ability itself, so a held Habilidade never counts toward its own
     * prerequisite. */
    @Test
    void isEligibleDoesNotCountTheAbilityItselfTowardItsOwnPrerequisite() {
        AventyrTitle title = new Santo(
                List.of(SantoSpecialization.ABENCOADO_PELA_LUZ),
                List.of(SantoAbility.PROTECAO_UNGIDA, SantoAbility.BASTIAO_DOS_NECESSITADOS));

        assertFalse(SantoAbility.BASTIAO_DOS_NECESSITADOS.isEligible(title),
                "1 other Habilidade held, not 2 — its own entry must not pad the count");
    }

    /** Protetor da Vida e da Morte wants 4, and the same mixed count applies. */
    @Test
    void isEligibleAppliesTheSameMixedCountToTheFourAbilitySuprema() {
        AventyrTitle title = new Santo(
                List.of(SantoSpecialization.ABENCOADO_PELA_LUZ),
                List.of(SantoAbility.PROTECAO_UNGIDA, SantoAbility.BASTIAO_DOS_NECESSITADOS,
                        AbencoadoPelaLuzAbility.ORGULHO_ELDURIANO));
        assertFalse(SantoAbility.PROTETOR_DA_VIDA_E_DA_MORTE.isEligible(title), "3 held, 4 required");

        AventyrTitle withFour = new Santo(
                List.of(SantoSpecialization.ABENCOADO_PELA_LUZ),
                List.of(SantoAbility.PROTECAO_UNGIDA, SantoAbility.BASTIAO_DOS_NECESSITADOS,
                        AbencoadoPelaLuzAbility.ORGULHO_ELDURIANO,
                        AbencoadoPelaLuzAbility.PELE_ROCHOSA_DE_EPONA));

        assertTrue(SantoAbility.PROTETOR_DA_VIDA_E_DA_MORTE.isEligible(withFour));
    }

    // getInteractionClass() is what AventyrTitle#activateAbility reflects on, so naming one is
    // exactly the claim "this Habilidade can actually be activated". Two constants make it.
    @Test
    void theTwoActivatedAbilitiesReportAnInteractionClass() {
        assertEquals(Optional.of(ProtecaoUngidaInteraction.class), SantoAbility.PROTECAO_UNGIDA.getInteractionClass());
        assertEquals(Optional.of(GuardaVidasInteraction.class), SantoAbility.GUARDA_VIDAS.getInteractionClass());
        // The other two are passive — scanned where they apply, never activated, so there is
        // nothing for an Interaction to do. (PROTETOR_DA_VIDA_E_DA_MORTE is additionally still
        // blocked on a damage-redirect and a locked-PV pool; see its own comment.)
        assertEquals(Optional.empty(), SantoAbility.BASTIAO_DOS_NECESSITADOS.getInteractionClass());
        assertEquals(Optional.empty(), SantoAbility.PROTETOR_DA_VIDA_E_DA_MORTE.getInteractionClass());
    }
}
