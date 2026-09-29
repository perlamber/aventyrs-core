package org.aventyrs.core.defect;

import org.aventyrs.core.ability.DexterityAbility;
import org.aventyrs.core.ability.StrengthAbility;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.AttributeAbilityServiceImpl;
import org.aventyrs.core.character.services.CharacterCreationService;
import org.aventyrs.core.character.services.CharacterCreationServiceImpl;
import org.aventyrs.core.ego.SorteAdvantage;
import org.aventyrs.core.feat.DefeitoFeat;
import org.aventyrs.core.feat.QualidadeFeat;
import org.aventyrs.core.feat.StartingFeatSlot;
import org.aventyrs.core.magic.ElementalType;
import org.aventyrs.core.race.Gigantes;
import org.aventyrs.core.race.Human;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.skill.SkillGraduation;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.skill.attention.AttentionCompetencyAbility;
import org.aventyrs.core.skill.artes.ArtesCompetencyAbility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.aventyrs.core.util.TranslatableMessages.INVALID_DEFECT_SELECTION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefectsAndQualitiesCreationTest {

    private final CharacterCreationService creation = new CharacterCreationServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static Character.CharacterBuilder base(final SkillType... trained) {
        return CharacterFixture.blank(CharacterFixture.BLANK)
                .skills(Arrays.stream(trained).collect(Collectors.toMap(skill -> skill, skill -> CharacterSkill.builder()
                        .skill(skill.newSkillInstance())
                        .graduation(SkillGraduation.builder().graduationValue(1).build()).build())));
    }

    private static int graduation(final Character character, final SkillType skill) {
        CharacterSkill held = character.getSkills().get(skill);
        return held == null ? 0 : held.getGraduation().getGraduationValue();
    }

    /** A Leve Defeito with no choice, taking +1 Graduação in Artes. */
    private static HeldDefect memoriaLeve() {
        return HeldDefect.atCreation(Defect.MEMORIA_FRACA, DefectSeverity.LEVE, List.of(),
                SuperacaoBenefit.GRADUACAO_ADICIONAL, List.of(SkillType.ARTES));
    }

    private void refused(final Character character, final List<HeldDefect> defects, final List<HeldQuality> qualities) {
        IllegalOperationException ex = assertThrows(IllegalOperationException.class,
                () -> creation.applyDefectsAndQualities(character, defects, qualities));
        assertEquals(INVALID_DEFECT_SELECTION, ex.getMessage());
    }

    // ---- catalog --------------------------------------------------------------------------------

    @Test
    void twelveDefeitosAndTwelveQualidadesEachOpposingItsOwnDefeito() {
        assertEquals(12, Defect.values().length);
        assertEquals(12, Quality.values().length);
        assertEquals(36, DefeitoFeat.values().length);
        assertEquals(24, QualidadeFeat.values().length);
        assertEquals(Set.of(Defect.values()),
                Arrays.stream(Quality.values()).map(Quality::getOpposes).collect(Collectors.toSet()));
        assertEquals("Chato", Defect.COMPORTAMENTO_EXCENTRICO.effectAt(DefectSeverity.LEVE).getLevelName());
        assertEquals(List.of(QualidadeFeat.RADIANTE_MENOR, QualidadeFeat.RADIANTE_MAIOR),
                Quality.RADIANTE.effectsAt(QualityClass.MAIOR));
    }

    @Test
    void theSuperarCostsAreThreeFiveAndSeven() {
        assertEquals(List.of(3, 5, 7), Arrays.stream(DefectSeverity.values())
                .map(severity -> severity.getOvercomeCost().intValue()).toList());
        assertEquals(3, SuperacaoBenefit.optionsFor(DefectSeverity.LEVE).size());
        assertEquals(3, SuperacaoBenefit.optionsFor(DefectSeverity.MODERADO).size());
        assertEquals(5, SuperacaoBenefit.optionsFor(DefectSeverity.GRAVE).size());
    }

    // ---- the Defeito rules ----------------------------------------------------------------------

    @Test
    void defeitosAreOptional() {
        Character none = creation.applyDefectsAndQualities(base().build(), List.of(), List.of());
        assertTrue(none.getDefects().isEmpty());
    }

    @Test
    void atMostOnePerGravidadeAndNoDefeitoTwice() {
        HeldDefect memoria = memoriaLeve();
        HeldDefect restricao = HeldDefect.atCreation(Defect.RESTRICAO_MORAL, DefectSeverity.LEVE, List.of(),
                SuperacaoBenefit.GRADUACAO_ADICIONAL, List.of(SkillType.ATTENTION));
        refused(base(SkillType.ARTES, SkillType.ATTENTION).build(), List.of(memoria, restricao), List.of());

        HeldDefect memoriaModerada = HeldDefect.atCreation(Defect.MEMORIA_FRACA, DefectSeverity.MODERADO, List.of(),
                SuperacaoBenefit.TALENTO_GERAL, List.of());
        refused(base(SkillType.ARTES).build(), List.of(memoria, memoriaModerada), List.of());
    }

    @Test
    void aCreationDefeitoIsRecordedWithItsEffectHeld() {
        Character result = creation.applyDefectsAndQualities(base(SkillType.ARTES).build(), List.of(memoriaLeve()), List.of());

        HeldDefect held = result.getDefects().get(0);
        assertTrue(held.fromCreation());
        assertTrue(result.getFeats().contains(DefeitoFeat.MEMORIA_FRACA_LEVE));
        assertEquals(2, graduation(result, SkillType.ARTES), "Graduação adicional");
    }

    @Test
    void theSuperacaoMustBeOfItsGravidade() {
        refused(base().build(), List.of(HeldDefect.atCreation(Defect.MEMORIA_FRACA, DefectSeverity.LEVE, List.of(),
                SuperacaoBenefit.TALENTO_GERAL, List.of())), List.of());
        refused(base().build(), List.of(HeldDefect.atCreation(Defect.MEMORIA_FRACA, DefectSeverity.LEVE, List.of(),
                null, List.of())), List.of());
    }

    @Test
    void appliedOnlyOnce() {
        Character once = creation.applyDefectsAndQualities(base(SkillType.ARTES).build(), List.of(memoriaLeve()), List.of());
        refused(once, List.of(), List.of());
    }

    // ---- choices ----------------------------------------------------------------------------------

    @Test
    void eachDefeitoChoiceIsValidated() {
        refused(base().build(), List.of(HeldDefect.atCreation(Defect.FOBIA, DefectSeverity.MODERADO, List.of("  "),
                SuperacaoBenefit.TALENTO_GERAL, List.of())), List.of());
        Character fobia = creation.applyDefectsAndQualities(base().build(), List.of(HeldDefect.atCreation(Defect.FOBIA,
                DefectSeverity.MODERADO, List.of("aranhas"), SuperacaoBenefit.TALENTO_GERAL, List.of())), List.of());
        assertEquals("aranhas", fobia.getDefects().get(0).choice(String.class).orElseThrow());

        // Dependência also names its Atributo.
        refused(base().build(), List.of(HeldDefect.atCreation(Defect.DEFICIENCIA_FISICA, DefectSeverity.GRAVE,
                List.of(Limb.PERNAS), SuperacaoBenefit.HABILIDADE_DE_ATRIBUTO, List.of())), List.of());
        creation.applyDefectsAndQualities(base().build(), List.of(HeldDefect.atCreation(Defect.DEFICIENCIA_FISICA,
                DefectSeverity.GRAVE, List.of(Limb.PERNAS, AttributeDomain.STRENGTH),
                SuperacaoBenefit.HABILIDADE_DE_ATRIBUTO, List.of())), List.of());
    }

    @Test
    void aVulnerabilidadeElementalNamesTwoElementos() {
        refused(base().build(), List.of(HeldDefect.atCreation(Defect.VULNERABILIDADE, DefectSeverity.MODERADO,
                List.of(VulnerabilityKind.ELEMENTAL, ElementalType.FOGO), SuperacaoBenefit.TALENTO_GERAL, List.of())), List.of());

        Character result = creation.applyDefectsAndQualities(base().build(), List.of(HeldDefect.atCreation(
                Defect.VULNERABILIDADE, DefectSeverity.MODERADO,
                List.of(VulnerabilityKind.ELEMENTAL, ElementalType.FOGO, ElementalType.GELO),
                SuperacaoBenefit.TALENTO_GERAL, List.of())), List.of());
        assertEquals(List.of(ElementalType.FOGO, ElementalType.GELO),
                result.getDefects().get(0).choicesOf(ElementalType.class));
        creation.applyDefectsAndQualities(base().build(), List.of(HeldDefect.atCreation(Defect.VULNERABILIDADE,
                DefectSeverity.MODERADO, List.of(VulnerabilityKind.MAGICO), SuperacaoBenefit.TALENTO_GERAL, List.of())), List.of());
    }

    // ---- Superação picks --------------------------------------------------------------------------

    @Test
    void anExtraTrainingIsOfAnUntrainedPericia() {
        Character trained = creation.applyDefectsAndQualities(base().build(), List.of(HeldDefect.atCreation(
                Defect.MEMORIA_FRACA, DefectSeverity.LEVE, List.of(), SuperacaoBenefit.TREINAMENTO_ADICIONAL,
                List.of(SkillType.FURTIVIDADE))), List.of());
        assertEquals(1, graduation(trained, SkillType.FURTIVIDADE));

        refused(base(SkillType.FURTIVIDADE).build(), List.of(HeldDefect.atCreation(Defect.MEMORIA_FRACA,
                DefectSeverity.LEVE, List.of(), SuperacaoBenefit.TREINAMENTO_ADICIONAL, List.of(SkillType.FURTIVIDADE))), List.of());
        refused(base().build(), List.of(HeldDefect.atCreation(Defect.MEMORIA_FRACA,
                DefectSeverity.LEVE, List.of(), SuperacaoBenefit.GRADUACAO_ADICIONAL, List.of(SkillType.FURTIVIDADE))), List.of());
    }

    @Test
    void aTalentoSuperacaoAddsAStartingSlot() {
        Character human = base().race(new Human()).build();
        int before = creation.getStartingFeatSlots(human).size();

        Character general = creation.applyDefectsAndQualities(human, List.of(HeldDefect.atCreation(Defect.MEMORIA_FRACA,
                DefectSeverity.MODERADO, List.of(), SuperacaoBenefit.TALENTO_GERAL, List.of())), List.of());
        List<StartingFeatSlot> slots = creation.getStartingFeatSlots(general);
        assertEquals(before + 1, slots.size());
        assertEquals(StartingFeatSlot.Source.DEFECT, slots.get(slots.size() - 1).source());
        assertTrue(slots.get(slots.size() - 1).isGeneralOnly());

        Character any = creation.applyDefectsAndQualities(human, List.of(HeldDefect.atCreation(Defect.MEMORIA_FRACA,
                DefectSeverity.GRAVE, List.of(), SuperacaoBenefit.TALENTO_E_PERICIA, List.of(SkillType.ARTES))), List.of());
        assertFalse(creation.getStartingFeatSlots(any).get(before).isGeneralOnly(), "Talento qualquer");
        assertEquals(1, graduation(any, SkillType.ARTES), "an untrained Perícia is trained");
    }

    @Test
    void aSecondVantagemDeEgoGoesToAnEgoWithoutOne() {
        Character result = creation.applyDefectsAndQualities(base().build(), List.of(HeldDefect.atCreation(
                Defect.MEMORIA_FRACA, DefectSeverity.GRAVE, List.of(), SuperacaoBenefit.VANTAGEM_DE_EGO,
                List.of(SorteAdvantage.ACE))), List.of());
        assertEquals(SorteAdvantage.ACE, result.getEgoAdvantages().get(EgoDomain.SORTE));

        refused(base().egoAdvantage(EgoDomain.SORTE, SorteAdvantage.AS_NA_MANGA).build(), List.of(HeldDefect.atCreation(
                Defect.MEMORIA_FRACA, DefectSeverity.GRAVE, List.of(), SuperacaoBenefit.VANTAGEM_DE_EGO,
                List.of(SorteAdvantage.ACE))), List.of());
    }

    @Test
    void anExtraCompetenciaIsOfATrainedPericia() {
        Character result = creation.applyDefectsAndQualities(base(SkillType.ARTES).build(), List.of(HeldDefect.atCreation(
                Defect.MEMORIA_FRACA, DefectSeverity.GRAVE, List.of(), SuperacaoBenefit.HABILIDADE_DE_COMPETENCIA,
                List.of(ArtesCompetencyAbility.DOM_BARDICO))), List.of());
        assertTrue(SkillCompetencyAbility.allFor(result).contains(ArtesCompetencyAbility.DOM_BARDICO));

        refused(base().build(), List.of(HeldDefect.atCreation(Defect.MEMORIA_FRACA, DefectSeverity.GRAVE, List.of(),
                SuperacaoBenefit.HABILIDADE_DE_COMPETENCIA, List.of(ArtesCompetencyAbility.DOM_BARDICO))), List.of());
    }

    @Test
    void anExtraHabilidadeDeAtributoIsABonusSlotOnAnAtributoThatHasOne() {
        AttributeAbilityServiceImpl abilities = new AttributeAbilityServiceImpl();
        Character strong = base()
                .attributes(CharacterAttributes.builder()
                        .strength(AttributeValue.builder().domain(AttributeDomain.STRENGTH).base(3).build()).build())
                .attributeAbility(StrengthAbility.SUBJUGAR)
                .build();
        assertThrows(IllegalOperationException.class,
                () -> abilities.grantAttributeAbility(strong, StrengthAbility.ESTILHACADOR), "the base's one slot is full");

        Character withSlot = creation.applyDefectsAndQualities(strong, List.of(HeldDefect.atCreation(Defect.MEMORIA_FRACA,
                DefectSeverity.GRAVE, List.of(), SuperacaoBenefit.HABILIDADE_DE_ATRIBUTO, List.of())), List.of());
        assertEquals(java.util.Arrays.asList((AttributeDomain) null), withSlot.getBonusAttributeAbilitySlots());
        Character granted = abilities.grantAttributeAbility(withSlot, StrengthAbility.ESTILHACADOR).getCharacter();
        assertThrows(IllegalOperationException.class,
                () -> abilities.grantAttributeAbility(granted, StrengthAbility.BESTA_DE_CARGA), "spent");
        assertThrows(IllegalOperationException.class,
                () -> abilities.grantAttributeAbility(withSlot, DexterityAbility.APRESSADO), "Destreza hasn't reached its first slot");
    }

    // ---- Qualidades ---------------------------------------------------------------------------------

    @Test
    void aSuperacaoQualidadeMustMatchWhatTheBenefitGrants() {
        HeldDefect fobia = HeldDefect.atCreation(Defect.FOBIA, DefectSeverity.LEVE, List.of("aranhas"),
                SuperacaoBenefit.QUALIDADE_MENOR, List.of());
        refused(base().build(), List.of(fobia), List.of());
        refused(base().build(), List.of(fobia), List.of(new HeldQuality(Quality.RADIANTE, QualityClass.MAIOR, List.of(),
                QualitySource.SUPERACAO)));

        Character result = creation.applyDefectsAndQualities(base().build(), List.of(fobia),
                List.of(new HeldQuality(Quality.RADIANTE, QualityClass.MENOR, List.of(), QualitySource.SUPERACAO)));
        assertTrue(result.getFeats().contains(QualidadeFeat.RADIANTE_MENOR));
        assertFalse(result.getFeats().contains(QualidadeFeat.RADIANTE_MAIOR));
    }

    @Test
    void aMaiorHoldsItsMenorsEffectToo() {
        Character result = creation.applyDefectsAndQualities(base().build(), List.of(HeldDefect.atCreation(Defect.FOBIA,
                DefectSeverity.MODERADO, List.of("chuva"), SuperacaoBenefit.QUALIDADE_MAIOR, List.of())),
                List.of(new HeldQuality(Quality.RADIANTE, QualityClass.MAIOR, List.of(), QualitySource.SUPERACAO)));

        assertTrue(result.getFeats().containsAll(List.of(QualidadeFeat.RADIANTE_MENOR, QualidadeFeat.RADIANTE_MAIOR)));
    }

    @Test
    void aQualidadeOpposingAHeldDefeitoIsRefused() {
        refused(base().build(), List.of(HeldDefect.atCreation(Defect.COMPORTAMENTO_EXCENTRICO, DefectSeverity.LEVE,
                List.of(), SuperacaoBenefit.QUALIDADE_MENOR, List.of())),
                List.of(new HeldQuality(Quality.RADIANTE, QualityClass.MENOR, List.of(), QualitySource.SUPERACAO)));
    }

    @Test
    void neverMoreThanThreeQualidades() {
        refused(base().build(), List.of(
                        HeldDefect.atCreation(Defect.FOBIA, DefectSeverity.MODERADO, List.of("chuva"),
                                SuperacaoBenefit.DUAS_QUALIDADES_MENORES, List.of()),
                        HeldDefect.atCreation(Defect.MEMORIA_FRACA, DefectSeverity.GRAVE, List.of(),
                                SuperacaoBenefit.QUALIDADE_MAIOR_E_MENOR, List.of())),
                List.of(new HeldQuality(Quality.RADIANTE, QualityClass.MENOR, List.of(), QualitySource.SUPERACAO),
                        new HeldQuality(Quality.SAUDE_DE_FERRO, QualityClass.MENOR, List.of(), QualitySource.SUPERACAO),
                        new HeldQuality(Quality.CENTELHA_MAIOR, QualityClass.MENOR, List.of(), QualitySource.SUPERACAO),
                        new HeldQuality(Quality.ESCOLHIDO_DA_MAGIA, QualityClass.MAIOR, List.of(), QualitySource.SUPERACAO)));
    }

    @Test
    void aTradedQualidadeNeedsADefeitoAndCostsGeneralSlots() {
        Character human = base(SkillType.ARTES).race(new Human()).build();
        HeldQuality tradedMaior = new HeldQuality(Quality.SAUDE_DE_FERRO, QualityClass.MAIOR, List.of(),
                QualitySource.GENERAL_FEAT_TRADE);
        refused(human, List.of(), List.of(tradedMaior));

        int generalBefore = (int) creation.getStartingFeatSlots(human).stream().filter(StartingFeatSlot::isGeneralOnly).count();
        Character result = creation.applyDefectsAndQualities(human, List.of(memoriaLeve()), List.of(tradedMaior));
        assertEquals(generalBefore - 2,
                creation.getStartingFeatSlots(result).stream().filter(StartingFeatSlot::isGeneralOnly).count());

        Character giant = base(SkillType.ARTES).race(new Gigantes()).build();
        assertEquals(2, creation.getTradableGeneralFeatSlots(new Gigantes()));
        refused(giant, List.of(memoriaLeve()), List.of(tradedMaior,
                new HeldQuality(Quality.RADIANTE, QualityClass.MENOR, List.of(), QualitySource.GENERAL_FEAT_TRADE)));
    }

    @Test
    void privilegiadoAndPrecognicaoGrantTheirPoints() {
        Character result = creation.applyDefectsAndQualities(base().build(), List.of(HeldDefect.atCreation(Defect.FOBIA,
                        DefectSeverity.GRAVE, List.of("ratos"), SuperacaoBenefit.QUALIDADE_MAIOR_E_MENOR, List.of())),
                List.of(new HeldQuality(Quality.DESTINADO_A_FORTUNA, QualityClass.MAIOR, List.of(), QualitySource.SUPERACAO),
                        new HeldQuality(Quality.RADIANTE, QualityClass.MENOR, List.of(), QualitySource.SUPERACAO)));
        assertEquals(base().build().getEgos().getEgo(EgoDomain.RECURSOS).getBase() + 1,
                result.getEgos().getEgo(EgoDomain.RECURSOS).getBase());

        Character seer = creation.applyDefectsAndQualities(base().build(), List.of(HeldDefect.atCreation(Defect.FOBIA,
                        DefectSeverity.MODERADO, List.of("ratos"), SuperacaoBenefit.QUALIDADE_MAIOR, List.of())),
                List.of(new HeldQuality(Quality.SEXTO_SENTIDO, QualityClass.MAIOR,
                        List.of(AttentionCompetencyAbility.PERCEPCAO_DE_FOXM), QualitySource.SUPERACAO)));
        assertEquals(base().build().getEgos().getEgo(EgoDomain.INICIATIVA).getBase() + 1,
                seer.getEgos().getEgo(EgoDomain.INICIATIVA).getBase());
        assertTrue(SkillCompetencyAbility.allFor(seer).contains(AttentionCompetencyAbility.PERCEPCAO_DE_FOXM));
    }

    @Test
    void tendenciaAtleticaMaiorIsASlotOnItsOwnAtributo() {
        Character result = creation.applyDefectsAndQualities(base().build(), List.of(HeldDefect.atCreation(Defect.FOBIA,
                        DefectSeverity.MODERADO, List.of("ratos"), SuperacaoBenefit.QUALIDADE_MAIOR, List.of())),
                List.of(new HeldQuality(Quality.TENDENCIA_ATLETICA, QualityClass.MAIOR, List.of(AttributeDomain.DEXTERITY),
                        QualitySource.SUPERACAO)));

        assertEquals(List.of(AttributeDomain.DEXTERITY), result.getBonusAttributeAbilitySlots());
        AttributeAbilityServiceImpl abilities = new AttributeAbilityServiceImpl();
        assertTrue(abilities.grantAttributeAbility(result, DexterityAbility.APRESSADO).getCharacter()
                .getAttributeAbilities().contains(DexterityAbility.APRESSADO), "no base required for Divinal");
        assertThrows(IllegalOperationException.class, () -> abilities.grantAttributeAbility(result, StrengthAbility.SUBJUGAR));
    }
}
