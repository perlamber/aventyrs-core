package org.aventyrs.core.feat;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.skill.SkillGraduation;
import org.aventyrs.core.skill.SkillSpecialization;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.skill.atletismo.Atletismo;
import org.aventyrs.core.skill.atletismo.AtletismoCompetencyAbility;
import org.aventyrs.core.skill.atletismo.AtletismoSpecialization;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pré-requisito clauses whose TODOs said they could not be expressed, although the mechanism
 * already existed: "recém-criados" ({@code Feat#isAcquirableOnlyAtCreation}) and a named
 * Especialização ({@code FeatRequirements#requiredSkillTraits}).
 */
class PrerequisiteSweepTest {

    /** Every Talento whose talentos.txt Pré-requisito reads "Apenas … recém-criados/as", whole. */
    private static final List<Feat> CREATION_ONLY = List.of(
            DestinoFeat.DESPERTAR_ANTECIPADO,
            DraconicoFeat.ARMAMENTO_DRACONICO, DraconicoFeat.ASAS_DE_DRAGAO,
            ElficoFeat.GUARDIAO_DOS_BOSQUES, ElficoFeat.GUARDIAO_DAS_DUNAS,
            ElficoFeat.GUARDIAO_DAS_NUVENS, ElficoFeat.GUARDIAO_DAS_PROFUNDEZAS,
            GnomoFeat.DUENDE,
            MesticoFeat.CARACTERISTICA_RACIAL_ADICIONAL,
            MonstruosoFeat.ANATOMIA_INCOMUM, MonstruosoFeat.OSSOS_OCOS,
            FeericoFeat.FAUNO, FeericoFeat.LUPERCAL, FeericoFeat.PIXIE, FeericoFeat.SIRENIDEO,
            GorgonaFeat.MARCA_DA_MALDICAO, GorgonaFeat.ACOLHIDA_POR_FLORA);

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    @Test
    void everyRecemCriadosTalentoIsCreationOnly() {
        CREATION_ONLY.forEach(feat -> assertTrue(feat.isAcquirableOnlyAtCreation(), String.valueOf(feat)));
    }

    @Test
    void noOtherCatalogTalentoIsCreationOnly() {
        List<Feat> others = FeatCatalog.all().stream().filter(feat -> !CREATION_ONLY.contains(feat)).toList();

        others.forEach(feat -> assertFalse(feat.isAcquirableOnlyAtCreation(), String.valueOf(feat)));
    }

    // ---------- named Especializações ----------

    private static Character athlete(final List<SkillSpecialization> specializations,
                                     final AtletismoCompetencyAbility... abilities) {
        return CharacterFixture.blank(CharacterFixture.BLANK)
                .skill(SkillType.ATLETISMO, CharacterSkill.builder()
                        .skill(new Atletismo())
                        .graduation(SkillGraduation.builder().graduationValue(4).build())
                        .specializations(specializations)
                        .build())
                .skillCompetencyAbilities(new ArrayList<>(Stream.of(abilities).toList()))
                .feats(new ArrayList<>())
                .build();
    }

    /** "Treinamento em Atletismo, Especialização Pulmão de Aço e a Habilidade de Competência Anfíbio". */
    @Test
    void criancaDoMarNeedsBothTheEspecializacaoAndTheHabilidade() {
        assertTrue(PeritoFeat.CRIANCA_DO_MAR.isEligible(
                athlete(List.of(AtletismoSpecialization.PULMAO_DE_ACO), AtletismoCompetencyAbility.ANFIBIO)));
        assertFalse(PeritoFeat.CRIANCA_DO_MAR.isEligible(athlete(List.of(), AtletismoCompetencyAbility.ANFIBIO)));
        assertFalse(PeritoFeat.CRIANCA_DO_MAR.isEligible(athlete(List.of(AtletismoSpecialization.PULMAO_DE_ACO))));
    }

    @Test
    void reiDaMontanhaNeedsLevantamentoDePesoAndAlpinistaVeloz() {
        assertTrue(PeritoFeat.REI_DA_MONTANHA.isEligible(athlete(List.of(AtletismoSpecialization.LEVANTAMENTO_DE_PESO),
                AtletismoCompetencyAbility.ALPINISTA_VELOZ)));
        assertFalse(PeritoFeat.REI_DA_MONTANHA.isEligible(athlete(List.of(), AtletismoCompetencyAbility.ALPINISTA_VELOZ)));
    }

    @Test
    void investidaAquaticaNeedsTriatleta() {
        assertTrue(MobilidadeFeat.INVESTIDA_AQUATICA.isEligible(athlete(List.of(AtletismoSpecialization.TRI_ATLETA))));
        assertFalse(MobilidadeFeat.INVESTIDA_AQUATICA.isEligible(athlete(List.of())));
    }
}
