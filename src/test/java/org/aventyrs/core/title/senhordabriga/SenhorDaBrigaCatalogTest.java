package org.aventyrs.core.title.senhordabriga;

import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.title.AventyrTitleAbility;
import org.aventyrs.core.title.PDCost;
import org.aventyrs.core.title.TitleCatalog;
import org.aventyrs.core.title.santo.Santo;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The catalog data of all three Senhor da Briga ability enums and the two Especializações. */
class SenhorDaBrigaCatalogTest {

    @Test
    void theCatalogHoldsTwoEspecializacoesAndSixteenTraitsBeyondThem() {
        assertEquals(2, SenhorDaBrigaSpecialization.values().length);
        assertEquals(4, SenhorDaBrigaAbility.values().length);
        assertEquals(4, PunhoInigualavelAbility.values().length);
        assertEquals(4, FantasmaDoRingueAbility.values().length);
    }

    @Test
    void everyTraitHasADescription() {
        Stream.of(SenhorDaBrigaSpecialization.values(), SenhorDaBrigaAbility.values(),
                        PunhoInigualavelAbility.values(), FantasmaDoRingueAbility.values())
                .flatMap(Stream::of)
                .forEach(trait -> assertFalse(((AventyrTitleAbility) trait).getDescription().isBlank()));
    }

    @Test
    void bothEspecializacoesArePassive() {
        for (SenhorDaBrigaSpecialization specialization : SenhorDaBrigaSpecialization.values()) {
            assertTrue(specialization.isPassive());
            assertEquals(Optional.empty(), specialization.getInteractionClass());
        }
    }

    @Test
    void costsFollowTheRulesText() {
        assertEquals(PDCost.fixed(1), SenhorDaBrigaAbility.FINALIZACAO.getPDCost());
        assertEquals(ActionCost.ofActionPoints(1), SenhorDaBrigaAbility.FINALIZACAO.getActionPointCost());
        assertEquals(PDCost.fixed(1), PunhoInigualavelAbility.IMPACTO_ELEMENTAL.getPDCost());
        assertEquals(ActionCost.ofActionPoints(2), PunhoInigualavelAbility.IMPACTO_ELEMENTAL.getActionPointCost());
        assertEquals(ActionCost.FREE_ACTION, PunhoInigualavelAbility.ROLAMENTO_OFENSIVO.getActionPointCost());
        assertEquals(PDCost.fixed(2), PunhoInigualavelAbility.AGARRAR_E_DERRUBAR.getPDCost());
        assertEquals(ActionCost.ofActionPoints(3), PunhoInigualavelAbility.AGARRAR_E_DERRUBAR.getActionPointCost());
        assertEquals(ActionCost.REACTION, FantasmaDoRingueAbility.CRUZ_DE_SANGUE.getActionPointCost());
        assertEquals(PDCost.variable(1), FantasmaDoRingueAbility.FINGIR_FRAQUEZAS.getPDCost());
    }

    @Test
    void passivesAreThePassiveOnesAndSupremasAreTheSupremaOnes() {
        List<AventyrTitleAbility> passives = List.of(SenhorDaBrigaAbility.CHAMAR_PRA_BRIGA,
                SenhorDaBrigaAbility.PUNHO_DE_FERRO, SenhorDaBrigaAbility.CAMPEAO_DA_TAVERNA,
                PunhoInigualavelAbility.GRANDE_MESTRE_DAS_BRIGAS, FantasmaDoRingueAbility.ENTRE_AS_PERNAS,
                FantasmaDoRingueAbility.MALICIA_DE_VALENTAO);
        passives.forEach(trait -> assertTrue(trait.isPassive(), trait.toString()));
        List<AventyrTitleAbility> supremas = List.of(SenhorDaBrigaAbility.PUNHO_DE_FERRO,
                SenhorDaBrigaAbility.CAMPEAO_DA_TAVERNA, PunhoInigualavelAbility.GRANDE_MESTRE_DAS_BRIGAS,
                FantasmaDoRingueAbility.MALICIA_DE_VALENTAO);
        Stream.of(SenhorDaBrigaAbility.values(), PunhoInigualavelAbility.values(), FantasmaDoRingueAbility.values())
                .flatMap(Stream::of)
                .forEach(trait -> assertEquals(supremas.contains(trait), ((AventyrTitleAbility) trait).isSupreme(),
                        trait.toString()));
    }

    @Test
    void everyActivatedTraitNamesItsInteraction() {
        assertEquals(Optional.of(FinalizacaoInteraction.class), SenhorDaBrigaAbility.FINALIZACAO.getInteractionClass());
        assertEquals(Optional.of(ImpactoElementalInteraction.class),
                PunhoInigualavelAbility.IMPACTO_ELEMENTAL.getInteractionClass());
        assertEquals(Optional.of(RolamentoOfensivoInteraction.class),
                PunhoInigualavelAbility.ROLAMENTO_OFENSIVO.getInteractionClass());
        assertEquals(Optional.of(AgarrarEDerrubarInteraction.class),
                PunhoInigualavelAbility.AGARRAR_E_DERRUBAR.getInteractionClass());
        assertEquals(Optional.of(CruzDeSangueInteraction.class),
                FantasmaDoRingueAbility.CRUZ_DE_SANGUE.getInteractionClass());
        assertEquals(Optional.of(FingirFraquezasInteraction.class),
                FantasmaDoRingueAbility.FINGIR_FRAQUEZAS.getInteractionClass());
    }

    @Test
    void titleLevelPrerequisitesAreOneEspecializacaoAndPunhoDeFerroAlsoNeedsTwoOthers() {
        for (SenhorDaBrigaAbility ability : SenhorDaBrigaAbility.values()) {
            assertEquals(1, ability.getRequiredSpecializations());
        }
        assertEquals(2, SenhorDaBrigaAbility.PUNHO_DE_FERRO.getRequiredOtherAbilities());
        assertEquals(0, SenhorDaBrigaAbility.CAMPEAO_DA_TAVERNA.getRequiredOtherAbilities());
    }

    @Test
    void gatedTraitsNameTheirEspecializacao() {
        for (PunhoInigualavelAbility ability : PunhoInigualavelAbility.values()) {
            assertEquals(Optional.of(SenhorDaBrigaSpecialization.PUNHO_INIGUALAVEL), ability.getRequiredSpecialization());
        }
        for (FantasmaDoRingueAbility ability : FantasmaDoRingueAbility.values()) {
            assertEquals(Optional.of(SenhorDaBrigaSpecialization.FANTASMA_DO_RINGUE), ability.getRequiredSpecialization());
        }
    }

    @Test
    void punhoDeFerroNeedsAnEspecializacaoAndTwoOtherHabilidades() {
        SenhorDaBriga bare = new SenhorDaBriga(List.of(), List.of());
        SenhorDaBriga oneOther = new SenhorDaBriga(List.of(SenhorDaBrigaSpecialization.PUNHO_INIGUALAVEL),
                List.of(SenhorDaBrigaAbility.FINALIZACAO));
        SenhorDaBriga twoOthers = new SenhorDaBriga(List.of(SenhorDaBrigaSpecialization.PUNHO_INIGUALAVEL),
                List.of(SenhorDaBrigaAbility.FINALIZACAO, PunhoInigualavelAbility.ROLAMENTO_OFENSIVO));

        assertFalse(SenhorDaBrigaAbility.PUNHO_DE_FERRO.isEligible(bare));
        assertFalse(SenhorDaBrigaAbility.PUNHO_DE_FERRO.isEligible(oneOther));
        assertTrue(SenhorDaBrigaAbility.PUNHO_DE_FERRO.isEligible(twoOthers));
    }

    @Test
    void grandeMestreCountsOnlyPunhoInigualavelHabilidades() {
        SenhorDaBriga withOtherCatalogs = new SenhorDaBriga(
                List.of(SenhorDaBrigaSpecialization.PUNHO_INIGUALAVEL, SenhorDaBrigaSpecialization.FANTASMA_DO_RINGUE),
                List.of(SenhorDaBrigaAbility.FINALIZACAO, FantasmaDoRingueAbility.CRUZ_DE_SANGUE,
                        PunhoInigualavelAbility.ROLAMENTO_OFENSIVO));
        SenhorDaBriga withTwoOwn = new SenhorDaBriga(List.of(SenhorDaBrigaSpecialization.PUNHO_INIGUALAVEL),
                List.of(PunhoInigualavelAbility.ROLAMENTO_OFENSIVO, PunhoInigualavelAbility.AGARRAR_E_DERRUBAR));

        assertFalse(PunhoInigualavelAbility.GRANDE_MESTRE_DAS_BRIGAS.isEligible(withOtherCatalogs));
        assertTrue(PunhoInigualavelAbility.GRANDE_MESTRE_DAS_BRIGAS.isEligible(withTwoOwn));
    }

    @Test
    void aGatedTraitNeedsItsOwnEspecializacao() {
        SenhorDaBriga fantasmaOnly = new SenhorDaBriga(List.of(SenhorDaBrigaSpecialization.FANTASMA_DO_RINGUE),
                List.of());

        assertFalse(PunhoInigualavelAbility.AGARRAR_E_DERRUBAR.isEligible(fantasmaOnly));
        assertTrue(FantasmaDoRingueAbility.FINGIR_FRAQUEZAS.isEligible(fantasmaOnly));
    }

    @Test
    void theFamilyIsRegisteredInTheCatalog() {
        assertTrue(TitleCatalog.all().stream().anyMatch(SenhorDaBriga.class::isInstance));
        assertTrue(TitleCatalog.all().stream().anyMatch(Santo.class::isInstance));
    }
}
