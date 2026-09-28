package org.aventyrs.core.background;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterEgos;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.CharacterCreationService;
import org.aventyrs.core.character.services.CharacterCreationServiceImpl;
import org.aventyrs.core.character.services.FeatServiceImpl;
import org.aventyrs.core.feat.AntecedenteFeat;
import org.aventyrs.core.feat.FeatChoice;
import org.aventyrs.core.magic.catalog.MagicTree;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.skill.SkillGraduation;
import org.aventyrs.core.skill.SkillTrait;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.skill.atletismo.AtletismoCompetencyAbility;
import org.aventyrs.core.skill.conhecimentos.ConhecimentosSpecialization;
import org.aventyrs.core.skill.dirigirecavalgar.DirigirECavalgarSpecialization;
import org.aventyrs.core.skill.empatiaselvagem.EmpatiaSelvagemSpecialization;
import org.aventyrs.core.skill.medicinaecura.MedicinaECuraCompetencyAbility;
import org.aventyrs.core.skill.medicinaecura.MedicinaECuraSpecialization;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.aventyrs.core.util.TranslatableMessages.INVALID_BACKGROUND_SELECTION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BackgroundCreationTest {

    private final CharacterCreationService creation = new CharacterCreationServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static Character blank() {
        return CharacterFixture.blank(CharacterFixture.BLANK).build();
    }

    private static Character trainedIn(final SkillType... skills) {
        return CharacterFixture.blank(CharacterFixture.BLANK)
                .skills(Arrays.stream(skills).collect(Collectors.toMap(skill -> skill, skill -> CharacterSkill.builder()
                        .skill(skill.newSkillInstance())
                        .graduation(SkillGraduation.builder().graduationValue(1).build())
                        .build())))
                .build();
    }

    private static int graduation(final Character character, final SkillType skill) {
        CharacterSkill held = character.getSkills().get(skill);
        return held == null ? 0 : held.getGraduation().getGraduationValue();
    }

    // ---- catalog ----------------------------------------------------------------------------------

    @Test
    void theCatalogHoldsTheTenAuthoredNaturalidadesAndTwentyTwoCarreiras() {
        assertEquals(10, Backgrounds.ofKind(BackgroundKind.ORIGIN).size());
        assertEquals(22, Backgrounds.ofKind(BackgroundKind.CAREER).size());
        assertEquals(Backgrounds.all().size(), AntecedenteFeat.values().length);
    }

    @Test
    void everyAntecedenteNamesItsOwnBenefit() {
        for (Background background : Backgrounds.all()) {
            assertEquals(background.name(), background.getBenefit().name());
        }
        assertEquals(OriginBackground.OFI, Backgrounds.byName("OFI").orElseThrow());
    }

    // ---- Graduações -------------------------------------------------------------------------------

    @Test
    void anUntrainedPericiaBecomesTrainedAndATrainedOneStacks() {
        Character result = creation.applyBackground(trainedIn(SkillType.ATLETISMO),
                AcquiredBackground.of(OriginBackground.DECIEMBRANO, List.of(SkillType.DIRIGIR_E_CAVALGAR),
                        List.of()));

        assertEquals(2, graduation(result, SkillType.ATLETISMO));
        assertEquals(1, graduation(result, SkillType.DIRIGIR_E_CAVALGAR));
    }

    @Test
    void theStoredRecordIsNormalizedWithTheFixedGrantsFilledIn() {
        Character result = creation.applyBackground(blank(),
                AcquiredBackground.of(OriginBackground.DECIEMBRANO, List.of(SkillType.DIRIGIR_E_CAVALGAR), List.of()));

        AcquiredBackground held = result.getBackground(BackgroundKind.ORIGIN).orElseThrow();
        assertEquals(List.of(SkillType.ATLETISMO, SkillType.DIRIGIR_E_CAVALGAR), held.graduationSkills());
        assertTrue(held.traits().contains(AtletismoCompetencyAbility.ANFIBIO));
        assertTrue(held.traits().contains(DirigirECavalgarSpecialization.AQUATICOS));
    }

    @Test
    void theInputCharacterIsLeftUntouched() {
        Character before = blank();
        creation.applyBackground(before,
                AcquiredBackground.of(OriginBackground.DECIEMBRANO, List.of(SkillType.DIRIGIR_E_CAVALGAR), List.of()));

        assertTrue(before.getSkills().isEmpty());
        assertTrue(before.getBackgrounds().isEmpty());
    }

    @Test
    void aPericiaOutsideTheOptionsOrTheWrongCountIsRefused() {
        IllegalOperationException outside = assertThrows(IllegalOperationException.class, () -> creation.applyBackground(
                blank(), AcquiredBackground.of(OriginBackground.HARENAI, List.of(SkillType.ARTES, SkillType.PERSUASAO), List.of())));
        assertEquals(INVALID_BACKGROUND_SELECTION, outside.getMessage());

        assertThrows(IllegalOperationException.class, () -> creation.applyBackground(
                blank(), AcquiredBackground.of(OriginBackground.HARENAI, List.of(SkillType.ARTES), List.of())));
    }

    @Test
    void aSecondAntecedenteOfTheSameKindIsRefused() {
        Character once = creation.applyBackground(blank(),
                AcquiredBackground.of(CareerBackground.BATEDOR, List.of(), List.of()));

        assertThrows(IllegalOperationException.class, () -> creation.applyBackground(once,
                AcquiredBackground.of(CareerBackground.ACADEMICO_AVENTYR, List.of(), List.of())));
    }

    @Test
    void applyBackgroundsTakesTheNaturalidadeThenTheCarreira() {
        Character result = creation.applyBackgrounds(blank(),
                AcquiredBackground.of(OriginBackground.SUDITO_DO_DRAGAO, List.of(),
                        List.of(EmpatiaSelvagemSpecialization.ANIMAIS_MUNDANOS,
                                org.aventyrs.core.skill.conhecimentos.ConhecimentosCompetencyAbility.GENERALISTA)),
                AcquiredBackground.of(CareerBackground.BATEDOR, List.of(), List.of()));

        assertEquals(2, result.getBackgrounds().size());
        assertEquals(1, graduation(result, SkillType.CONHECIMENTOS));
        assertEquals(1, graduation(result, SkillType.ATTENTION));

        assertThrows(IllegalOperationException.class, () -> creation.applyBackgrounds(blank(),
                AcquiredBackground.of(CareerBackground.BATEDOR, List.of(), List.of()),
                AcquiredBackground.of(OriginBackground.OFI, List.of(SkillType.ATTENTION, SkillType.PROFISSAO), List.of())));
    }

    // ---- "Se treinado em X" ----------------------------------------------------------------------

    @Test
    void seTreinadoReadsTrainedAtAllIncludingEarlierCreationTraining() {
        List<TraitGrant> grants = creation.getBackgroundTraitGrants(trainedIn(SkillType.EMPATIA_SELVAGEM),
                OriginBackground.DECIEMBRANO, List.of(SkillType.DIRIGIR_E_CAVALGAR));

        List<SkillTrait> offered = grants.stream().flatMap(grant -> grant.options().stream()).toList();
        assertTrue(offered.contains(DirigirECavalgarSpecialization.AQUATICOS), "picked with the +1");
        assertTrue(offered.contains(EmpatiaSelvagemSpecialization.ANIMAIS_MUNDANOS), "already trained");
    }

    @Test
    void anUntrainedConditionOwesNothing() {
        List<TraitGrant> grants = creation.getBackgroundTraitGrants(blank(),
                OriginBackground.DECIEMBRANO, List.of(SkillType.DIRIGIR_E_CAVALGAR));

        assertTrue(grants.stream().flatMap(grant -> grant.options().stream())
                .noneMatch(EmpatiaSelvagemSpecialization.ANIMAIS_MUNDANOS::equals));
    }

    // ---- chosen traits -----------------------------------------------------------------------------

    @Test
    void aChoiceGrantMustBeAnsweredExactly() {
        assertThrows(IllegalOperationException.class, () -> creation.applyBackground(blank(),
                AcquiredBackground.of(CareerBackground.CURANDEIRO, List.of(), List.of(MedicinaECuraSpecialization.ALQUIMIA))));

        Character result = creation.applyBackground(blank(), AcquiredBackground.of(CareerBackground.CURANDEIRO, List.of(),
                List.of(MedicinaECuraSpecialization.ALQUIMIA, MedicinaECuraCompetencyAbility.BOM_DOUTOR)));

        assertTrue(result.getSpecializations(SkillType.MEDICINA_E_CURA).contains(MedicinaECuraSpecialization.ALQUIMIA));
        assertTrue(SkillCompetencyAbility.allFor(result).contains(MedicinaECuraCompetencyAbility.BOM_DOUTOR));
    }

    @Test
    void aTraitNotOfferedIsRefused() {
        assertThrows(IllegalOperationException.class, () -> creation.applyBackground(blank(),
                AcquiredBackground.of(CareerBackground.CURANDEIRO, List.of(), List.of(MedicinaECuraSpecialization.ALQUIMIA,
                        MedicinaECuraCompetencyAbility.BOM_DOUTOR, ConhecimentosSpecialization.NATUREZA))));
    }

    @Test
    void aHeldTraitIsNeverOfferedAgain() {
        Character holding = CharacterFixture.blank(CharacterFixture.BLANK)
                .skills(Map.of(SkillType.MEDICINA_E_CURA, CharacterSkill.builder()
                        .skill(SkillType.MEDICINA_E_CURA.newSkillInstance())
                        .specializations(List.of(MedicinaECuraSpecialization.ALQUIMIA))
                        .graduation(SkillGraduation.builder().graduationValue(1).build()).build()))
                .build();

        List<TraitGrant> grants = creation.getBackgroundTraitGrants(holding, CareerBackground.CURANDEIRO, List.of());

        assertTrue(grants.stream().flatMap(grant -> grant.options().stream())
                .noneMatch(MedicinaECuraSpecialization.ALQUIMIA::equals));
    }

    @Test
    void negociadorsEspecializacaoFollowsThePericiaChosen() {
        List<TraitGrant> attention = creation.getBackgroundTraitGrants(blank(), CareerBackground.NEGOCIADOR,
                List.of(SkillType.ATTENTION));

        assertEquals(1, attention.size());
        assertEquals(org.aventyrs.core.skill.attention.AttentionSpecialization.DISCERNIR_MOTIVACAO,
                attention.get(0).options().get(0));
    }

    // ---- Ego --------------------------------------------------------------------------------------

    @Test
    void anEgoPointRaisesTheBaseAndCountsTowardTheVantagemThreshold() {
        Character before = blank();
        assertFalse(creation.isEgoAdvantageAvailable(EgoDomain.SORTE, before.getEgos()));

        Character after = creation.applyBackground(before, AcquiredBackground.of(CareerBackground.APOSTADOR, List.of(), List.of()));

        CharacterEgos egos = after.getEgos();
        assertEquals(before.getEgos().getEgo(EgoDomain.SORTE).getBase() + 1, egos.getEgo(EgoDomain.SORTE).getBase());
        assertTrue(creation.isEgoAdvantageAvailable(EgoDomain.SORTE, egos));
    }

    // ---- Benefício choices ------------------------------------------------------------------------

    @Test
    void theBenefitPicksAreValidatedAgainstTheirOptions() {
        List<FeatChoice<?>> choices = creation.getBackgroundBenefitChoices(blank(), CareerBackground.ESCUDEIRO);
        assertEquals(SkillType.class, choices.get(0).type());

        assertThrows(IllegalOperationException.class, () -> creation.applyBackground(blank(),
                new AcquiredBackground(CareerBackground.ESCUDEIRO, List.of(), List.of(), List.of(SkillType.ARTES))));
        assertThrows(IllegalOperationException.class, () -> creation.applyBackground(blank(),
                new AcquiredBackground(CareerBackground.ESCUDEIRO, List.of(), List.of(), List.of())));

        Character result = creation.applyBackground(blank(), new AcquiredBackground(CareerBackground.ESCUDEIRO,
                List.of(), List.of(), List.of(SkillType.ESQUIVA_E_APARAR)));
        assertEquals(SkillType.ESQUIVA_E_APARAR, result.getBackground(BackgroundKind.CAREER).orElseThrow()
                .benefitChoice(SkillType.class).orElseThrow());
    }

    @Test
    void estudiosoArcanoPicksAnArvore() {
        Character result = creation.applyBackground(blank(), new AcquiredBackground(CareerBackground.ESTUDIOSO_ARCANO,
                List.of(SkillType.CONHECIMENTOS), List.of(org.aventyrs.core.skill.conhecimentos.ConhecimentosCompetencyAbility.GENERALISTA),
                List.of(MagicTree.values()[0])));

        assertEquals(MagicTree.values()[0], result.getBackground(BackgroundKind.CAREER).orElseThrow()
                .benefitChoice(MagicTree.class).orElseThrow());
    }

    // ---- the Benefício as a held Talento ----------------------------------------------------------

    @Test
    void theBenefitIsHeldThroughTheAntecedenteAndNeverBought() {
        Character result = creation.applyBackground(blank(), AcquiredBackground.of(CareerBackground.BATEDOR, List.of(), List.of()));
        assertTrue(result.getFeats().contains(AntecedenteFeat.BATEDOR));

        CharacterSheet sheet = CharacterSheet.of(blank(), new Player());
        assertThrows(IllegalOperationException.class,
                () -> new FeatServiceImpl().grantFeat(sheet.getCharacter(), sheet, AntecedenteFeat.OFI));
        assertTrue(Set.copyOf(sheet.getCharacter().getFeats()).isEmpty());
    }
}
