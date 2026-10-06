package org.aventyrs.core.title.bruxo;

import org.aventyrs.core.title.AventyrTitleAbility;
import org.aventyrs.core.title.TitleCatalog;
import org.aventyrs.core.title.TitleIdentity;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BruxoCatalogTest {

    @Test
    void catalogSizes() {
        assertEquals(1, BruxoDespertar.values().length);
        assertEquals(2, BruxoSpecialization.values().length);
        assertEquals(4, BruxoAbility.values().length);
        assertEquals(4, IluminadoAbility.values().length);
        assertEquals(4, OraculoAbissalAbility.values().length);
    }

    @Test
    void everyTraitHasADescriptionAndNoActivationYet() {
        allTraits().forEach(trait -> {
            assertFalse(trait.getDescription().isBlank(), trait.name());
            assertEquals(Optional.empty(), trait.getInteractionClass(), trait.name());
        });
    }

    @Test
    void everyTraitButFamiliarMaiorAndInvocacaoMaiorTeachesAMisticismo() {
        allTraits().forEach(trait -> assertEquals(
                trait != BruxoAbility.FAMILIAR_MAIOR && trait != BruxoAbility.INVOCACAO_MAIOR,
                trait.getMisticismoFilter().isPresent(), trait.name()));
    }

    @Test
    void supremasAreFlagged() {
        assertTrue(BruxoAbility.INVOCACAO_DUPLA.isSupreme());
        assertTrue(BruxoAbility.PACTO_DE_CONJURACAO.isSupreme());
        assertTrue(IluminadoAbility.MALDICAO_DA_NEVASCA_DO_SUDOESTE.isSupreme());
        assertTrue(OraculoAbissalAbility.BENCAO_DOS_RAIOS_DO_NORDESTE.isSupreme());
        assertFalse(BruxoAbility.INVOCACAO_MAIOR.isSupreme());
        assertFalse(IluminadoAbility.BENCAO_DO_MAR_DO_SUL.isSupreme());
    }

    @Test
    void titleLevelHabilidadesNeedAnEspecializacao() {
        assertFalse(BruxoAbility.FAMILIAR_MAIOR.isEligible(bruxo(List.of())));
        assertTrue(BruxoAbility.FAMILIAR_MAIOR.isEligible(bruxo(List.of(BruxoSpecialization.ILUMINADO))));
    }

    @Test
    void invocacaoMaiorNeedsTwoOtherHabilidades() {
        List<BruxoSpecialization> one = List.of(BruxoSpecialization.ILUMINADO);
        assertFalse(BruxoAbility.INVOCACAO_MAIOR.isEligible(bruxo(one, BruxoAbility.FAMILIAR_MAIOR)));
        assertTrue(BruxoAbility.INVOCACAO_MAIOR.isEligible(
                bruxo(one, BruxoAbility.FAMILIAR_MAIOR, IluminadoAbility.BENCAO_DO_MAR_DO_SUL)));
    }

    @Test
    void especializacoesDoNotCountAsHabilidadesDeBruxo() {
        // Both Especializações and one Habilidade: only one Habilidade de Bruxo.
        Bruxo bruxo = bruxo(List.of(BruxoSpecialization.ILUMINADO, BruxoSpecialization.ORACULO_ABISSAL),
                BruxoAbility.FAMILIAR_MAIOR);

        assertEquals(1, bruxo.getBruxoAbilityCount());
        assertFalse(BruxoAbility.INVOCACAO_MAIOR.isEligible(bruxo));
    }

    @Test
    void invocacaoDuplaNeedsInvocacaoMaiorByNameAndSixHabilidades() {
        List<BruxoSpecialization> both = List.of(BruxoSpecialization.ILUMINADO, BruxoSpecialization.ORACULO_ABISSAL);
        AventyrTitleAbility[] sixWithout = {BruxoAbility.FAMILIAR_MAIOR, IluminadoAbility.BENCAO_DO_MAR_DO_SUL,
                IluminadoAbility.BENCAO_DO_VENTO_DO_LESTE, IluminadoAbility.MALDICAO_DO_PANTANO_DO_SUDESTE,
                OraculoAbissalAbility.MALDICAO_DO_DESERTO_DO_OESTE, OraculoAbissalAbility.MALDICAO_DAS_CHAMAS_DO_NORTE};
        assertFalse(BruxoAbility.INVOCACAO_DUPLA.isEligible(bruxo(both, sixWithout)));

        List<AventyrTitleAbility> five = new ArrayList<>(List.of(sixWithout).subList(0, 5));
        five.add(BruxoAbility.INVOCACAO_MAIOR);
        assertTrue(BruxoAbility.INVOCACAO_DUPLA.isEligible(bruxo(both, five.toArray(AventyrTitleAbility[]::new))));
        assertFalse(BruxoAbility.INVOCACAO_DUPLA.isEligible(
                bruxo(both, five.subList(1, 6).toArray(AventyrTitleAbility[]::new))));
    }

    @Test
    void especializacaoSupremasCountOnlyTheirOwnHabilidades() {
        List<BruxoSpecialization> both = List.of(BruxoSpecialization.ILUMINADO, BruxoSpecialization.ORACULO_ABISSAL);
        Bruxo mixed = bruxo(both, IluminadoAbility.BENCAO_DO_MAR_DO_SUL, OraculoAbissalAbility.MALDICAO_DO_DESERTO_DO_OESTE);
        Bruxo pure = bruxo(both, IluminadoAbility.BENCAO_DO_MAR_DO_SUL, IluminadoAbility.BENCAO_DO_VENTO_DO_LESTE);

        assertFalse(IluminadoAbility.MALDICAO_DA_NEVASCA_DO_SUDOESTE.isEligible(mixed));
        assertTrue(IluminadoAbility.MALDICAO_DA_NEVASCA_DO_SUDOESTE.isEligible(pure));
        assertEquals(2, pure.getAbilityCount(BruxoSpecialization.ILUMINADO));
        assertEquals(0, pure.getAbilityCount(BruxoSpecialization.ORACULO_ABISSAL));
    }

    @Test
    void bruxoIsInTheCatalogWithItsIdentity() {
        Bruxo bruxo = TitleCatalog.all().stream().filter(Bruxo.class::isInstance).map(Bruxo.class::cast)
                .findFirst().orElseThrow();

        assertEquals(Optional.of(TitleIdentity.BRUXO), bruxo.getIdentity());
        assertFalse(bruxo.getPrimaryTitleBonusDescription().isBlank());
    }

    @Test
    void everyTraitIsFoundByName() {
        allTraits().forEach(trait -> assertEquals(Optional.of(trait), Bruxo.teacherNamed(trait.name())));
    }

    private static Stream<MisticismoTeacher> allTraits() {
        return Stream.of(BruxoDespertar.values(), BruxoSpecialization.values(), BruxoAbility.values(),
                        IluminadoAbility.values(), OraculoAbissalAbility.values())
                .flatMap(Stream::of)
                .map(MisticismoTeacher.class::cast);
    }

    static Bruxo bruxo(final List<BruxoSpecialization> specializations, final AventyrTitleAbility... abilities) {
        return new Bruxo(specializations, List.of(abilities));
    }
}
