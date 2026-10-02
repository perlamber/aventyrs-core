package org.aventyrs.core.defect;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.DamageDescriptor;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.character.TitleSlot;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.CharacterCreationService;
import org.aventyrs.core.character.services.CharacterCreationServiceImpl;
import org.aventyrs.core.character.services.DamageServiceImpl;
import org.aventyrs.core.character.services.DeterminationPointsServiceImpl;
import org.aventyrs.core.character.services.HitPointsServiceImpl;
import org.aventyrs.core.character.services.InitiativeServiceImpl;
import org.aventyrs.core.character.services.MagicPointsServiceImpl;
import org.aventyrs.core.character.services.MovementServiceImpl;
import org.aventyrs.core.character.services.SkillGraduationServiceImpl;
import org.aventyrs.core.feat.DefeitoFeat;
import org.aventyrs.core.feat.Feat;
import org.aventyrs.core.feat.QualidadeFeat;
import org.aventyrs.core.magic.ElementalType;
import org.aventyrs.core.rest.RestServiceImpl;
import org.aventyrs.core.rest.RestType;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantAction;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillGraduation;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.title.TitleAcquisitionServiceImpl;
import org.aventyrs.core.title.santo.Santo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Plan Phase 2 — each Defeito/Qualidade clause wired onto an existing hook, tested by what it changes
 * on a character who took it through {@code CharacterCreationService#applyDefectsAndQualities}: a
 * before/after delta read off the consuming service (the {@code testing-a-feat} skill's shape).
 */
class DefectsAndQualitiesEffectTest {

    private final CharacterCreationService creation = new CharacterCreationServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    /** Trained at 1 in the Perícias these tests roll; Empatia Selvagem stays free for a Leve's Superação. */
    private static Character base() {
        return CharacterFixture.blank(CharacterFixture.BLANK)
                .skills(Arrays.stream(new SkillType[]{SkillType.ATTENTION, SkillType.PERSUASAO,
                                SkillType.ATAQUE_CORPO_A_CORPO, SkillType.CONHECIMENTOS, SkillType.ESQUIVA_E_APARAR})
                        .collect(Collectors.toMap(skill -> skill, skill -> CharacterSkill.builder()
                                .skill(skill.newSkillInstance())
                                .graduation(SkillGraduation.builder().graduationValue(1).build()).build())))
                .build();
    }

    /** base() holding defect at severity, with a Superação that touches nothing these tests read. */
    private Character withDefect(final Defect defect, final DefectSeverity severity, final Object... choices) {
        SuperacaoBenefit benefit = switch (severity) {
            case LEVE -> SuperacaoBenefit.TREINAMENTO_ADICIONAL;
            case MODERADO -> SuperacaoBenefit.TALENTO_GERAL;
            case GRAVE -> SuperacaoBenefit.HABILIDADE_DE_ATRIBUTO;
        };
        List<Object> picks = severity == DefectSeverity.LEVE ? List.of(SkillType.EMPATIA_SELVAGEM) : List.of();
        return creation.applyDefectsAndQualities(base(), List.of(HeldDefect.atCreation(defect, severity,
                List.of(choices), benefit, picks)), List.of());
    }

    /** base() holding quality at class, bought by a narrative carrier Defeito's Superação. */
    private Character withQuality(final Quality quality, final QualityClass qualityClass, final Object... choices) {
        Defect carrier = quality.getOpposes() == Defect.MEMORIA_FRACA ? Defect.RESTRICAO_MORAL : Defect.MEMORIA_FRACA;
        HeldDefect defect = qualityClass == QualityClass.MENOR
                ? HeldDefect.atCreation(carrier, DefectSeverity.LEVE, List.of(), SuperacaoBenefit.QUALIDADE_MENOR, List.of())
                : HeldDefect.atCreation(carrier, DefectSeverity.MODERADO, List.of(), SuperacaoBenefit.QUALIDADE_MAIOR, List.of());
        return creation.applyDefectsAndQualities(base(), List.of(defect),
                List.of(new HeldQuality(quality, qualityClass, List.of(choices), QualitySource.SUPERACAO)));
    }

    private static CharacterSheet sheet(final Character character) {
        return CharacterSheet.of(character, new Player());
    }

    private static int rollBonus(final Character character, final SkillType skill) {
        return skill.newInteraction().applyTo(sheet(character)).getSkillRollBonus();
    }

    private static int difficultyReduction(final Character character, final SkillType skill) {
        return skill.newInteraction().applyTo(sheet(character)).getDifficultyReduction();
    }

    private static SkillRoll activating(final Integer target, final Feat feat) {
        return new SkillRoll(List.of(2, 2, 2), null, target, null, null, Set.of(feat));
    }

    // ---- Carisma-based rolls ---------------------------------------------------------------------

    @Test
    void chatoTakesADesvantagemOnCarismaRollsOnly() {
        Character chato = withDefect(Defect.COMPORTAMENTO_EXCENTRICO, DefectSeverity.LEVE);

        assertEquals(rollBonus(base(), SkillType.PERSUASAO) + Skill.DISADVANTAGE_MALUS, rollBonus(chato, SkillType.PERSUASAO));
        assertEquals(rollBonus(base(), SkillType.ATTENTION), rollBonus(chato, SkillType.ATTENTION), "Atenção is not Carisma");
    }

    @Test
    void esquisitaoRaisesTheGdOfCarismaRollsByOneNivel() {
        Character esquisitao = withDefect(Defect.COMPORTAMENTO_EXCENTRICO, DefectSeverity.MODERADO);

        assertEquals(difficultyReduction(base(), SkillType.PERSUASAO) - 1, difficultyReduction(esquisitao, SkillType.PERSUASAO));
        assertEquals(difficultyReduction(base(), SkillType.ATTENTION), difficultyReduction(esquisitao, SkillType.ATTENTION));
    }

    @Test
    void amigavelGrantsVantagemOnCarismaRolls() {
        Character amigavel = withQuality(Quality.RADIANTE, QualityClass.MENOR);

        assertEquals(rollBonus(base(), SkillType.PERSUASAO) + Skill.ADVANTAGE_BONUS, rollBonus(amigavel, SkillType.PERSUASAO));
        assertEquals(rollBonus(base(), SkillType.ATTENTION), rollBonus(amigavel, SkillType.ATTENTION));
    }

    @Test
    void auraDeConfiancaEasesOneActivatedCarismaRollPerCena() {
        Character radiante = withQuality(Quality.RADIANTE, QualityClass.MAIOR);
        CharacterSheet sheet = sheet(radiante);
        int plain = difficultyReduction(radiante, SkillType.PERSUASAO);

        InteractionResult activated = SkillType.PERSUASAO.newInteraction()
                .applyTo(sheet, null, activating(18, QualidadeFeat.RADIANTE_MAIOR));
        assertEquals(plain + 1, activated.getDifficultyReduction());

        assertThrows(IllegalOperationException.class, () -> SkillType.ATTENTION.newInteraction()
                .applyTo(sheet, null, activating(18, QualidadeFeat.RADIANTE_MAIOR)), "only Carisma-based Perícias");

        sheet.recordAction(new CombatantAction(SkillType.PERSUASAO, AttributeDomain.CHARISMA, null, null, 0, null,
                null, Set.of(QualidadeFeat.RADIANTE_MAIOR)));
        assertThrows(IllegalOperationException.class, () -> SkillType.PERSUASAO.newInteraction()
                .applyTo(sheet, null, activating(18, QualidadeFeat.RADIANTE_MAIOR)), "once per Cena");
    }

    // ---- Atenção and Iniciativa ------------------------------------------------------------------

    @Test
    void percepcaoNubladaAndSentidoIneficienteWorsenAtencao() {
        assertEquals(rollBonus(base(), SkillType.ATTENTION) + Skill.DISADVANTAGE_MALUS,
                rollBonus(withDefect(Defect.DEFICIENCIA_SENSORIAL, DefectSeverity.LEVE, Sense.VISAO), SkillType.ATTENTION));
        assertEquals(difficultyReduction(base(), SkillType.ATTENTION) - 1,
                difficultyReduction(withDefect(Defect.DEFICIENCIA_SENSORIAL, DefectSeverity.MODERADO, Sense.VISAO),
                        SkillType.ATTENTION));
    }

    @Test
    void everyDisturbioDeAtencaoLevelTakesDesvantagemOnAtencaoAndIniciativa() {
        InitiativeServiceImpl initiative = new InitiativeServiceImpl();
        for (DefectSeverity severity : DefectSeverity.values()) {
            Character disturbed = withDefect(Defect.DISTURBIO_DE_ATENCAO, severity);
            assertEquals(rollBonus(base(), SkillType.ATTENTION) + Skill.DISADVANTAGE_MALUS,
                    rollBonus(disturbed, SkillType.ATTENTION), severity.name());
            assertEquals(initiative.getTotalInitiative(base()) + Skill.DISADVANTAGE_MALUS,
                    initiative.getTotalInitiative(disturbed), severity.name());
        }
    }

    @Test
    void atencaoSobrenaturalGrantsVantagemOnAtencaoAndIniciativa() {
        Character sexto = withQuality(Quality.SEXTO_SENTIDO, QualityClass.MENOR);
        InitiativeServiceImpl initiative = new InitiativeServiceImpl();

        assertEquals(rollBonus(base(), SkillType.ATTENTION) + Skill.ADVANTAGE_BONUS, rollBonus(sexto, SkillType.ATTENTION));
        assertEquals(initiative.getTotalInitiative(base()) + Skill.ADVANTAGE_BONUS, initiative.getTotalInitiative(sexto));
    }

    @Test
    void sempreAlertaGrantsVantagemOnAtencao() {
        assertEquals(rollBonus(base(), SkillType.ATTENTION) + Skill.ADVANTAGE_BONUS,
                rollBonus(withQuality(Quality.SENTIDO_SUPERIOR, QualityClass.MENOR, Sense.AUDICAO), SkillType.ATTENTION));
    }

    @Test
    void sentirOTodoChoosesSuccessOnAnAtencaoRollOncePerSession() {
        CharacterSheet sheet = sheet(withQuality(Quality.SENTIDO_SUPERIOR, QualityClass.MAIOR, Sense.AUDICAO));

        InteractionResult plain = SkillType.ATTENTION.newInteraction()
                .applyTo(sheet, null, new SkillRoll(List.of(2, 2, 2), null, 30));
        assertFalse(plain.getSucceeded(), "6 plus a small bonus never reaches 30");

        InteractionResult chosen = SkillType.ATTENTION.newInteraction()
                .applyTo(sheet, null, activating(30, QualidadeFeat.SENTIDO_SUPERIOR_MAIOR));
        assertTrue(chosen.getSucceeded());

        sheet.recordAction(new CombatantAction(SkillType.ATTENTION, AttributeDomain.INSTINCT, null, null, 0, null,
                null, Set.of(QualidadeFeat.SENTIDO_SUPERIOR_MAIOR)));
        assertThrows(IllegalOperationException.class, () -> SkillType.ATTENTION.newInteraction()
                .applyTo(sheet, null, activating(30, QualidadeFeat.SENTIDO_SUPERIOR_MAIOR)), "spent for the session");
    }

    @Test
    void essenciaMalandraGrantsVantagemOnAtencaoAndPersuasao() {
        Character malandro = withQuality(Quality.OPORTUNISTA_NATO, QualityClass.MAIOR);

        assertEquals(rollBonus(base(), SkillType.ATTENTION) + Skill.ADVANTAGE_BONUS, rollBonus(malandro, SkillType.ATTENTION));
        assertEquals(rollBonus(base(), SkillType.PERSUASAO) + Skill.ADVANTAGE_BONUS, rollBonus(malandro, SkillType.PERSUASAO));
        assertEquals(rollBonus(base(), SkillType.CONHECIMENTOS), rollBonus(malandro, SkillType.CONHECIMENTOS));
    }

    // ---- Deficiência Física ----------------------------------------------------------------------

    @Test
    void danoPermanenteInTheArmsWorsensArmRollsButNotLegRolls() {
        Character arms = withDefect(Defect.DEFICIENCIA_FISICA, DefectSeverity.LEVE, Limb.BRACOS);

        assertEquals(rollBonus(base(), SkillType.ATAQUE_CORPO_A_CORPO) + Skill.DISADVANTAGE_MALUS,
                rollBonus(arms, SkillType.ATAQUE_CORPO_A_CORPO));
        assertEquals(rollBonus(base(), SkillType.ESQUIVA_E_APARAR), rollBonus(arms, SkillType.ESQUIVA_E_APARAR));
        assertEquals(new MovementServiceImpl().getMovementBase(base()), new MovementServiceImpl().getMovementBase(arms));
    }

    @Test
    void danoPermanenteInTheLegsWorsensLegRollsAndCostsTwoUd() {
        Character legs = withDefect(Defect.DEFICIENCIA_FISICA, DefectSeverity.LEVE, Limb.PERNAS);
        MovementServiceImpl movement = new MovementServiceImpl();

        assertEquals(rollBonus(base(), SkillType.ESQUIVA_E_APARAR) + Skill.DISADVANTAGE_MALUS,
                rollBonus(legs, SkillType.ESQUIVA_E_APARAR));
        assertEquals(rollBonus(base(), SkillType.ATAQUE_CORPO_A_CORPO), rollBonus(legs, SkillType.ATAQUE_CORPO_A_CORPO));
        assertEquals(movement.getMovementBase(base()) - 2, movement.getMovementBase(legs));
    }

    /** "Desvantagem em suas rolagens de Danos Físicos" — arms only, and never on a Magia's damage. */
    @Test
    void danoPermanenteInTheArmsTakesADesvantagemOnPhysicalDamage() {
        Character arms = withDefect(Defect.DEFICIENCIA_FISICA, DefectSeverity.LEVE, Limb.BRACOS);
        Character legs = withDefect(Defect.DEFICIENCIA_FISICA, DefectSeverity.LEVE, Limb.PERNAS);

        assertEquals(Skill.DISADVANTAGE_MALUS, DefeitoFeat.DEFICIENCIA_FISICA_LEVE
                .resolveDamageBonus(SkillType.ATAQUE_CORPO_A_CORPO, null, null, arms, null).orElseThrow().getValue());
        assertTrue(DefeitoFeat.DEFICIENCIA_FISICA_LEVE
                .resolveDamageBonus(SkillType.ATAQUE_CORPO_A_CORPO, null, null, legs, null).isEmpty());
    }

    // ---- Multiplicadores -------------------------------------------------------------------------

    @Test
    void corpoFragilLowersTheLifeMultiplier() {
        HitPointsServiceImpl hitPoints = new HitPointsServiceImpl();
        Character baseline = base();

        assertEquals(hitPoints.getLifeMultiplier(baseline) - 1,
                hitPoints.getLifeMultiplier(withDefect(Defect.CORPO_FRAGIL, DefectSeverity.LEVE)));
        assertEquals(hitPoints.getLifeMultiplier(baseline) - 2,
                hitPoints.getLifeMultiplier(withDefect(Defect.CORPO_FRAGIL, DefectSeverity.MODERADO)));
    }

    @Test
    void inaptoParaMagiasLowersTheManaMultiplierAndAlmaDeAetherRaisesIt() {
        MagicPointsServiceImpl magicPoints = new MagicPointsServiceImpl();
        int before = magicPoints.getManaMultiplier(base());

        assertEquals(before - 2, magicPoints.getManaMultiplier(withDefect(Defect.DESCONEXAO_COM_O_AETHER, DefectSeverity.LEVE)));
        assertEquals(before + 1, magicPoints.getManaMultiplier(withQuality(Quality.ESCOLHIDO_DA_MAGIA, QualityClass.MAIOR)));
    }

    @Test
    void herancaDeGilgameshLowersTheDeterminationMultiplierAndSonhoDeGilgameshRaisesIt() {
        DeterminationPointsServiceImpl determination = new DeterminationPointsServiceImpl();
        int before = determination.getDeterminationMultiplier(base());

        assertEquals(before - 1, determination.getDeterminationMultiplier(
                withDefect(Defect.HERANCA_DE_GILGAMESH, DefectSeverity.LEVE)));
        assertEquals(before - 2, determination.getDeterminationMultiplier(
                withDefect(Defect.HERANCA_DE_GILGAMESH, DefectSeverity.MODERADO)));
        assertEquals(before + 1, determination.getDeterminationMultiplier(
                withQuality(Quality.CENTELHA_MAIOR, QualityClass.MAIOR)), "no Regalia equipped: +1, not +2");
    }

    @Test
    void constituicaoInabalavelRaisesTheLifeMultiplier() {
        HitPointsServiceImpl hitPoints = new HitPointsServiceImpl();
        assertEquals(hitPoints.getLifeMultiplier(base()) + 1,
                hitPoints.getLifeMultiplier(withQuality(Quality.SAUDE_DE_FERRO, QualityClass.MAIOR)));
    }

    // ---- Descanso --------------------------------------------------------------------------------

    @Test
    void vigorosoRecoversOneMorePvOnALongoOrStrongerRestAndOnePerTitulo() {
        RestServiceImpl rest = new RestServiceImpl();
        Character vigoroso = withQuality(Quality.SAUDE_DE_FERRO, QualityClass.MENOR);

        assertEquals(rest.getRecoveredHitPoints(base(), RestType.CURTO), rest.getRecoveredHitPoints(vigoroso, RestType.CURTO));
        assertEquals(rest.getRecoveredHitPoints(base(), RestType.LONGO) + 1, rest.getRecoveredHitPoints(vigoroso, RestType.LONGO));
        assertEquals(rest.getRecoveredHitPoints(base(), RestType.TOTAL) + 1, rest.getRecoveredHitPoints(vigoroso, RestType.TOTAL));

        Character titled = withQuality(Quality.SAUDE_DE_FERRO, QualityClass.MENOR);
        titled.grantTitle(new Santo(List.of(), List.of()), TitleSlot.PRIMARY);
        Character titledBase = base();
        titledBase.grantTitle(new Santo(List.of(), List.of()), TitleSlot.PRIMARY);
        assertEquals(rest.getRecoveredHitPoints(titledBase, RestType.LONGO) + 2,
                rest.getRecoveredHitPoints(titled, RestType.LONGO));
    }

    @Test
    void constituicaoInabalavelAddsTwoMorePvOnTopOfVigoroso() {
        RestServiceImpl rest = new RestServiceImpl();
        Character maior = withQuality(Quality.SAUDE_DE_FERRO, QualityClass.MAIOR);
        Character menor = withQuality(Quality.SAUDE_DE_FERRO, QualityClass.MENOR);

        assertEquals(rest.getRecoveredHitPoints(menor, RestType.LONGO) + 2, rest.getRecoveredHitPoints(maior, RestType.LONGO));
    }

    // ---- Magia -----------------------------------------------------------------------------------

    @Test
    void linhagemArcanaGrantsOneInstanceOfRm() {
        DamageServiceImpl damage = new DamageServiceImpl();
        assertEquals(damage.getTotalMagicReduction(sheet(base())) + 2,
                damage.getTotalMagicReduction(sheet(withQuality(Quality.ESCOLHIDO_DA_MAGIA, QualityClass.MENOR))));
        assertEquals(Skill.ADVANTAGE_BONUS, QualidadeFeat.ESCOLHIDO_DA_MAGIA_MENOR.resolveCastingRollBonus(null, base(), Set.of()));
    }

    // ---- Resistência Atípica ---------------------------------------------------------------------

    @Test
    void herancaDraconicaHalvesTheChosenElementOnly() {
        CharacterSheet resistant = sheet(withQuality(Quality.RESISTENCIA_ATIPICA, QualityClass.MENOR,
                EnergyKind.ELEMENTAL, ElementalType.FOGO));

        assertTrue(resistant.halvesDamage(DamageType.ELEMENTAL, new DamageDescriptor(DamageType.ELEMENTAL, ElementalType.FOGO)));
        assertFalse(resistant.halvesDamage(DamageType.ELEMENTAL, new DamageDescriptor(DamageType.ELEMENTAL, ElementalType.GELO)));
        assertFalse(resistant.isImmuneToDamage(DamageType.ELEMENTAL, new DamageDescriptor(DamageType.ELEMENTAL, ElementalType.FOGO)));
    }

    @Test
    void herancaDivinaIsImmuneToTheChosenElement() {
        CharacterSheet immune = sheet(withQuality(Quality.RESISTENCIA_ATIPICA, QualityClass.MAIOR,
                EnergyKind.ELEMENTAL, ElementalType.FOGO));

        assertTrue(immune.isImmuneToDamage(DamageType.ELEMENTAL, new DamageDescriptor(DamageType.ELEMENTAL, ElementalType.FOGO)));
        assertFalse(immune.isImmuneToDamage(DamageType.ELEMENTAL, new DamageDescriptor(DamageType.ELEMENTAL, ElementalType.GELO)));
    }

    @Test
    void aProfanaResistanceHalvesNoElement() {
        CharacterSheet profana = sheet(withQuality(Quality.RESISTENCIA_ATIPICA, QualityClass.MENOR, EnergyKind.PROFANA));
        for (ElementalType element : ElementalType.values()) {
            if (element != ElementalType.TODOS) {
                assertFalse(profana.halvesDamage(DamageType.ELEMENTAL, new DamageDescriptor(DamageType.ELEMENTAL, element)));
            }
        }
    }

    // ---- Graduações ------------------------------------------------------------------------------

    @Test
    void dominarPadroesTakesHalfAnExpOffGraduacoesUpToTheThird() {
        SkillGraduationServiceImpl graduations = new SkillGraduationServiceImpl();
        Character padroes = withQuality(Quality.INTELECTO_SUPERIOR, QualityClass.MAIOR);

        // Graduação 1 → 2 is inside the window; the discount is the whole difference.
        assertEquals(graduations.getUpgradeCost(base(), SkillType.ATTENTION).subtract(new BigDecimal("0.5")),
                graduations.getUpgradeCost(padroes, SkillType.ATTENTION));
        assertEquals(BigDecimal.ZERO.compareTo(QualidadeFeat.INTELECTO_SUPERIOR_MAIOR
                .resolveGraduationCostReduction(padroes, SkillType.ATTENTION, 4)), 0, "the fourth pays full price");
    }

    // ---- Títulos ---------------------------------------------------------------------------------

    @Test
    void herancaDeGilgameshClosesTheSecondarySlotOrEveryTitulo() {
        TitleAcquisitionServiceImpl titles = new TitleAcquisitionServiceImpl();
        Santo santo = new Santo(List.of(), List.of());

        Character leve = withDefect(Defect.HERANCA_DE_GILGAMESH, DefectSeverity.LEVE);
        assertTrue(titles.isPermitted(leve, santo, TitleSlot.PRIMARY));
        assertFalse(titles.isPermitted(leve, santo, TitleSlot.SECONDARY));
        assertThrows(IllegalOperationException.class, () -> titles.grantTitle(leve, santo, TitleSlot.SECONDARY));

        Character grave = withDefect(Defect.HERANCA_DE_GILGAMESH, DefectSeverity.GRAVE);
        assertFalse(titles.isPermitted(grave, santo, TitleSlot.PRIMARY));
        assertFalse(titles.isPermitted(grave, santo));

        assertTrue(titles.isPermitted(base(), santo, TitleSlot.SECONDARY), "control");
    }

    // ---- Controls --------------------------------------------------------------------------------

    /** The narrative Defeitos change nothing a service reports. */
    @Test
    void narrativeDefeitosChangeNoFigure() {
        HitPointsServiceImpl hitPoints = new HitPointsServiceImpl();
        InitiativeServiceImpl initiative = new InitiativeServiceImpl();
        for (Defect defect : List.of(Defect.MEMORIA_FRACA, Defect.RESTRICAO_MORAL)) {
            for (DefectSeverity severity : DefectSeverity.values()) {
                Character held = withDefect(defect, severity);
                assertEquals(rollBonus(base(), SkillType.PERSUASAO), rollBonus(held, SkillType.PERSUASAO));
                assertEquals(rollBonus(base(), SkillType.ATTENTION), rollBonus(held, SkillType.ATTENTION));
                assertEquals(hitPoints.getLifeMultiplier(base()), hitPoints.getLifeMultiplier(held));
                assertEquals(initiative.getTotalInitiative(base()), initiative.getTotalInitiative(held));
            }
        }
    }
}
