package org.aventyrs.core.title.santo;

import org.aventyrs.core.title.AventyrTitle;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.title.PDCost;
import org.junit.jupiter.api.Test;

import java.util.List;
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
        assertFalse(AbencoadoPelaLuzAbility.CORPO_INDESTRUTIVEL_DE_EPONA.isSupreme());
        assertTrue(AbencoadoPelaLuzAbility.GLORIA_RELAMPEJANTE_DE_TESLA.isSupreme());
    }

    @Test
    void orgulhoEldurianoReportsItsMinimumVariableCost() {
        assertEquals(PDCost.variable(2), AbencoadoPelaLuzAbility.ORGULHO_ELDURIANO.getPDCost());
        assertEquals(ActionCost.ofActionPoints(3), AbencoadoPelaLuzAbility.ORGULHO_ELDURIANO.getActionPointCost());
    }

    @Test
    void gritoDeGuerraVulcanoHasTheRightActivationCost() {
        assertEquals(PDCost.fixed(2), AbencoadoPelaLuzAbility.GRITO_DE_GUERRA_VULCANO.getPDCost());
        assertEquals(ActionCost.ofActionPoints(1), AbencoadoPelaLuzAbility.GRITO_DE_GUERRA_VULCANO.getActionPointCost());
    }

    @Test
    void corpoIndestrutivelDeEponaHasTheRightActivationCost() {
        assertEquals(PDCost.fixed(4), AbencoadoPelaLuzAbility.CORPO_INDESTRUTIVEL_DE_EPONA.getPDCost());
        assertEquals(ActionCost.ofActionPoints(1), AbencoadoPelaLuzAbility.CORPO_INDESTRUTIVEL_DE_EPONA.getActionPointCost());
    }

    @Test
    void gloriaRelampejanteDeTeslaIsAFreeActionActivation() {
        assertEquals(PDCost.fixed(3), AbencoadoPelaLuzAbility.GLORIA_RELAMPEJANTE_DE_TESLA.getPDCost());
        assertEquals(ActionCost.FREE_ACTION, AbencoadoPelaLuzAbility.GLORIA_RELAMPEJANTE_DE_TESLA.getActionPointCost());
    }

    @Test
    void noAbilityIsPassive() {
        // All four have an explicit activation (PA, or Ação Livre for GLORIA_RELAMPEJANTE_DE_TESLA,
        // which spends no PA but is still player-triggered) — none is "Custo de Ativação: Nenhum,
        // habilidade passiva".
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

    @Test
    void everyAbilityRequiresTheAbencoadoPelaLuzEspecializacao() {
        for (AbencoadoPelaLuzAbility ability : AbencoadoPelaLuzAbility.values()) {
            assertEquals(Optional.of(SantoSpecialization.ABENCOADO_PELA_LUZ), ability.getRequiredSpecialization());
        }
    }

    @Test
    void onlyGloriaRelampejanteDeTeslaRequiresOtherAbilities() {
        assertEquals(0, AbencoadoPelaLuzAbility.ORGULHO_ELDURIANO.getRequiredOtherAbilities());
        assertEquals(0, AbencoadoPelaLuzAbility.GRITO_DE_GUERRA_VULCANO.getRequiredOtherAbilities());
        assertEquals(0, AbencoadoPelaLuzAbility.CORPO_INDESTRUTIVEL_DE_EPONA.getRequiredOtherAbilities());
        assertEquals(2, AbencoadoPelaLuzAbility.GLORIA_RELAMPEJANTE_DE_TESLA.getRequiredOtherAbilities());
    }

    @Test
    void isEligibleRejectsATitleWithoutTheEspecializacao() {
        AventyrTitle title = new Santo(List.of(SantoSpecialization.ABRACADO_PELA_ESCURIDAO), List.of());

        for (AbencoadoPelaLuzAbility ability : AbencoadoPelaLuzAbility.values()) {
            assertFalse(ability.isEligible(title));
        }
    }

    @Test
    void isEligibleAcceptsOrgulhoEldurianoOnceTheEspecializacaoIsHeld() {
        AventyrTitle title = new Santo(List.of(SantoSpecialization.ABENCOADO_PELA_LUZ), List.of());

        assertTrue(AbencoadoPelaLuzAbility.ORGULHO_ELDURIANO.isEligible(title));
    }

    @Test
    void isEligibleRejectsGloriaRelampejanteDeTeslaWithoutEnoughSiblingAbilities() {
        AventyrTitle title = new Santo(
                List.of(SantoSpecialization.ABENCOADO_PELA_LUZ),
                List.of(AbencoadoPelaLuzAbility.ORGULHO_ELDURIANO));

        assertFalse(AbencoadoPelaLuzAbility.GLORIA_RELAMPEJANTE_DE_TESLA.isEligible(title));
    }

    /**
     * The narrow reading, and the reason the scope is per-constant rather than global: this
     * constant's clause names one <i>Especialização</i> ("2 Habilidades de 'Abençoado pela Luz'"),
     * unlike {@code SantoAbility}'s, which names the Título and counts every Habilidade held.
     * A Título-level Habilidade is not a Habilidade de 'Abençoado pela Luz' and must not count.
     */
    @Test
    void isEligibleForGloriaRelampejanteDeTeslaIgnoresHabilidadesNotGatedOnItsEspecializacao() {
        AventyrTitle title = new Santo(
                List.of(SantoSpecialization.ABENCOADO_PELA_LUZ),
                List.of(SantoAbility.PROTECAO_UNGIDA, SantoAbility.BASTIAO_DOS_NECESSITADOS));

        assertFalse(AbencoadoPelaLuzAbility.GLORIA_RELAMPEJANTE_DE_TESLA.isEligible(title));
    }

    /** Nor does one gated on the *other* Especialização — "de 'Abençoado pela Luz'" is specific. */
    @Test
    void isEligibleForGloriaRelampejanteDeTeslaIgnoresTheOtherEspecializacaosHabilidades() {
        AventyrTitle title = new Santo(
                List.of(SantoSpecialization.ABENCOADO_PELA_LUZ, SantoSpecialization.ABRACADO_PELA_ESCURIDAO),
                List.of(AbracadoPelaEscuridaoAbility.ESPINHOS_VENENOS_DE_GAEA,
                        AbracadoPelaEscuridaoAbility.FUROR_DE_SYLPH));

        assertFalse(AbencoadoPelaLuzAbility.GLORIA_RELAMPEJANTE_DE_TESLA.isEligible(title));
    }

    @Test
    void isEligibleAcceptsGloriaRelampejanteDeTeslaOnceEnoughSiblingAbilitiesAreHeld() {
        AventyrTitle title = new Santo(
                List.of(SantoSpecialization.ABENCOADO_PELA_LUZ),
                List.of(AbencoadoPelaLuzAbility.ORGULHO_ELDURIANO, AbencoadoPelaLuzAbility.CORPO_INDESTRUTIVEL_DE_EPONA));

        assertTrue(AbencoadoPelaLuzAbility.GLORIA_RELAMPEJANTE_DE_TESLA.isEligible(title));
    }

    // Three of the four are activatable through AventyrTitle#activateAbility. Only
    // CORPO_INDESTRUTIVEL_DE_EPONA has nothing to point to: its RA is grantable now, but the first-hit
    // negation that gates it has no mechanism (see the constant's own comment).
    @Test
    void onlyTheActivatableAbilitiesReportAnInteractionClass() {
        assertEquals(Optional.of(OrgulhoEldurianoInteraction.class), AbencoadoPelaLuzAbility.ORGULHO_ELDURIANO.getInteractionClass());
        assertEquals(Optional.of(GritoDeGuerraVulcanoInteraction.class), AbencoadoPelaLuzAbility.GRITO_DE_GUERRA_VULCANO.getInteractionClass());
        assertEquals(Optional.of(CorpoIndestrutivelDeEponaInteraction.class),
                AbencoadoPelaLuzAbility.CORPO_INDESTRUTIVEL_DE_EPONA.getInteractionClass());
        assertEquals(Optional.of(GloriaRelampejanteDeTeslaInteraction.class),
                AbencoadoPelaLuzAbility.GLORIA_RELAMPEJANTE_DE_TESLA.getInteractionClass());
    }
}
