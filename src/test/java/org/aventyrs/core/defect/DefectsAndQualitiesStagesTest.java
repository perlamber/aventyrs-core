package org.aventyrs.core.defect;

import org.aventyrs.core.ability.StrengthAbility;
import org.aventyrs.core.action.ActionPointsServiceImpl;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.DamageDescriptor;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.TitleSlot;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.AttributeAbilityServiceImpl;
import org.aventyrs.core.character.services.CharacterCreationService;
import org.aventyrs.core.character.services.CharacterCreationServiceImpl;
import org.aventyrs.core.character.services.DamageServiceImpl;
import org.aventyrs.core.character.services.DefenseServiceImpl;
import org.aventyrs.core.character.services.DeterminationPointsServiceImpl;
import org.aventyrs.core.character.services.FreeActionsServiceImpl;
import org.aventyrs.core.character.services.HitPointsServiceImpl;
import org.aventyrs.core.character.services.MagicPointsServiceImpl;
import org.aventyrs.core.character.services.MovementServiceImpl;
import org.aventyrs.core.character.services.ReactionsServiceImpl;
import org.aventyrs.core.combat.AttackReceiver;
import org.aventyrs.core.combat.IncomingAttack;
import org.aventyrs.core.feat.QualidadeFeat;
import org.aventyrs.core.magic.ElementalType;
import org.aventyrs.core.magic.MagicType;
import org.aventyrs.core.magic.catalog.ProfanarSpell;
import org.aventyrs.core.magic.catalog.VidaSpell;
import org.aventyrs.core.rest.RestServiceImpl;
import org.aventyrs.core.rest.RestType;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantAction;
import org.aventyrs.core.sheet.Condition;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.DifficultyLevel;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillGraduation;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.skill.esquivaeaparar.EsquivaEApararInteraction;
import org.aventyrs.core.title.TitleAcquisitionServiceImpl;
import org.aventyrs.core.title.santo.Santo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.aventyrs.core.util.TranslatableMessages.ATTRIBUTE_ABILITY_FORBIDDEN;
import static org.aventyrs.core.util.TranslatableMessages.SKILL_USE_PREVENTED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Plan Phase 3 — the Defeito/Qualidade clauses that needed a new stage: the fixed multiplier, automatic
 * failure, a refused Perícia, halved movement, Rodada parity, typed vulnerability, Condição substitution and
 * Duração, and the per-combat cost relief. Same shape as {@link DefectsAndQualitiesEffectTest}.
 */
class DefectsAndQualitiesStagesTest {

    private final CharacterCreationService creation = new CharacterCreationServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static Character base() {
        return CharacterFixture.blank(CharacterFixture.BLANK)
                .skills(Arrays.stream(new SkillType[]{SkillType.ATTENTION, SkillType.PERSUASAO,
                                SkillType.ATAQUE_CORPO_A_CORPO, SkillType.CONHECIMENTOS, SkillType.ESQUIVA_E_APARAR})
                        .collect(Collectors.toMap(skill -> skill, skill -> CharacterSkill.builder()
                                .skill(skill.newSkillInstance())
                                .graduation(SkillGraduation.builder().graduationValue(1).build()).build())))
                .build();
    }

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

    private static SceneContext combatRound(final int round) {
        return new SceneContext(List.of(), List.of(), Map.of(), null, true, round, false);
    }

    private static Boolean succeeds(final Character character, final SkillType skill) {
        return skill.newInteraction().applyTo(sheet(character), null, new SkillRoll(List.of(6, 6, 5), null, 10))
                .getSucceeded();
    }

    // ---- The fixed multiplier (Sobreposição) -----------------------------------------------------

    @Test
    void theGraveMultipliersAreAlwaysOne() {
        assertEquals(1, new HitPointsServiceImpl().getLifeMultiplier(withDefect(Defect.CORPO_FRAGIL, DefectSeverity.GRAVE)));
        assertEquals(1, new MagicPointsServiceImpl().getManaMultiplier(
                withDefect(Defect.DESCONEXAO_COM_O_AETHER, DefectSeverity.MODERADO)));
        assertEquals(1, new DeterminationPointsServiceImpl().getDeterminationMultiplier(
                withDefect(Defect.HERANCA_DE_GILGAMESH, DefectSeverity.GRAVE)));
    }

    @Test
    void aFixedMultiplierIgnoresATimedBonusToo() {
        Character fragile = withDefect(Defect.CORPO_FRAGIL, DefectSeverity.GRAVE);
        CharacterSheet sheet = sheet(fragile);
        sheet.grantTemporaryBonus(org.aventyrs.core.modifier.ModifierType.LIFE_MULTIPLIER, 3, 2);

        assertEquals(1, new HitPointsServiceImpl().getLifeMultiplier(fragile, sheet));
    }

    @Test
    void nulificadorRecoversNoPmOnAnyDescanso() {
        RestServiceImpl rest = new RestServiceImpl();
        Character nulificador = withDefect(Defect.DESCONEXAO_COM_O_AETHER, DefectSeverity.GRAVE);

        for (RestType type : RestType.values()) {
            assertEquals(0, rest.getRecoveredMagicPoints(nulificador, type), type.name());
        }
        assertEquals(rest.getRecoveredHitPoints(base(), RestType.LONGO), rest.getRecoveredHitPoints(nulificador, RestType.LONGO));
    }

    // ---- Automatic failure -----------------------------------------------------------------------

    @Test
    void oLoucoFailsEveryCarismaRollAndOnlyThose() {
        Character louco = withDefect(Defect.COMPORTAMENTO_EXCENTRICO, DefectSeverity.GRAVE);

        assertTrue(succeeds(base(), SkillType.PERSUASAO), "17 beats 10");
        assertFalse(succeeds(louco, SkillType.PERSUASAO));
        assertTrue(succeeds(louco, SkillType.ATTENTION));
    }

    @Test
    void ausenciaSensorialFailsEveryAtencaoRoll() {
        Character absent = withDefect(Defect.DEFICIENCIA_SENSORIAL, DefectSeverity.GRAVE, Sense.VISAO);

        assertFalse(succeeds(absent, SkillType.ATTENTION));
        assertTrue(succeeds(absent, SkillType.PERSUASAO));
    }

    // ---- Membro Ausente / Dependência ------------------------------------------------------------

    @Test
    void membroAusenteRefusesTheLimbsPericias() {
        CharacterSheet arms = sheet(withDefect(Defect.DEFICIENCIA_FISICA, DefectSeverity.MODERADO, Limb.BRACOS));

        IllegalOperationException refused = assertThrows(IllegalOperationException.class,
                () -> SkillType.ATAQUE_CORPO_A_CORPO.newInteraction().applyTo(arms));
        assertEquals(SKILL_USE_PREVENTED, refused.getMessage());
        SkillType.ESQUIVA_E_APARAR.newInteraction().applyTo(arms);
    }

    @Test
    void membroAusenteInTheLegsHalvesMovimentoBase() {
        MovementServiceImpl movement = new MovementServiceImpl();
        assertEquals(movement.getMovementBase(base()) / 2,
                movement.getMovementBase(withDefect(Defect.DEFICIENCIA_FISICA, DefectSeverity.MODERADO, Limb.PERNAS)));
        assertEquals(movement.getMovementBase(base()),
                movement.getMovementBase(withDefect(Defect.DEFICIENCIA_FISICA, DefectSeverity.MODERADO, Limb.BRACOS)));
    }

    @Test
    void dependenciaForbidsHabilidadesOfTheRolledAtributo() {
        Character dependent = withDefect(Defect.DEFICIENCIA_FISICA, DefectSeverity.GRAVE, Limb.PERNAS, AttributeDomain.STRENGTH);

        IllegalOperationException refused = assertThrows(IllegalOperationException.class,
                () -> new AttributeAbilityServiceImpl().grantAttributeAbility(dependent, StrengthAbility.SUBJUGAR));
        assertEquals(ATTRIBUTE_ABILITY_FORBIDDEN, refused.getMessage());
        assertThrows(IllegalOperationException.class, () -> SkillType.ESQUIVA_E_APARAR.newInteraction()
                .applyTo(sheet(dependent)), "Membro Ausente's refusal is cumulative");
    }

    // ---- Centelha Dormente -----------------------------------------------------------------------

    @Test
    void centelhaDormenteAwakensThePrimarioOnlyFromTwentyFiveExp() {
        TitleAcquisitionServiceImpl titles = new TitleAcquisitionServiceImpl();
        Santo santo = new Santo(List.of(), List.of());
        Character dormant = withDefect(Defect.HERANCA_DE_GILGAMESH, DefectSeverity.MODERADO);
        CharacterSheet sheet = sheet(dormant);

        assertFalse(titles.isPermitted(dormant, santo, TitleSlot.PRIMARY), "no sheet: cannot tell");
        sheet.accumulateExperience(BigDecimal.valueOf(24));
        assertFalse(titles.isPermitted(dormant, sheet, santo, TitleSlot.PRIMARY));
        sheet.accumulateExperience(BigDecimal.ONE);
        assertTrue(titles.isPermitted(dormant, sheet, santo, TitleSlot.PRIMARY));
        assertFalse(titles.isPermitted(dormant, sheet, santo, TitleSlot.SECONDARY));
    }

    // ---- Rodada parity ---------------------------------------------------------------------------

    @Test
    void semFocoLosesAPontoDeAcaoInOddCombatRodadas() {
        ActionPointsServiceImpl actionPoints = new ActionPointsServiceImpl();
        CharacterSheet plain = sheet(base());
        CharacterSheet unfocused = sheet(withDefect(Defect.DISTURBIO_DE_ATENCAO, DefectSeverity.MODERADO));

        assertEquals(actionPoints.getMaxActionPoints(plain, 2, combatRound(3)) - 1,
                actionPoints.getMaxActionPoints(unfocused, 2, combatRound(3)));
        assertEquals(actionPoints.getMaxActionPoints(plain, 1, combatRound(2)),
                actionPoints.getMaxActionPoints(unfocused, 1, combatRound(2)));
        SceneContext outsideCombat = new SceneContext(List.of(), List.of(), Map.of(), null, false, 1, false);
        assertEquals(actionPoints.getMaxActionPoints(plain, 0, outsideCombat),
                actionPoints.getMaxActionPoints(unfocused, 0, outsideCombat));
    }

    @Test
    void presoAImaginacaoHasNoReacoesOrAcoesLivresInOddRodadas() {
        CharacterSheet trapped = sheet(withDefect(Defect.DISTURBIO_DE_ATENCAO, DefectSeverity.GRAVE));
        ReactionsServiceImpl reactions = new ReactionsServiceImpl();
        FreeActionsServiceImpl freeActions = new FreeActionsServiceImpl();

        assertEquals(0, reactions.getTotalReactions(trapped, 0, combatRound(1)));
        assertEquals(0, freeActions.getTotalFreeActions(trapped, 0, combatRound(1)));
        assertEquals(reactions.getTotalReactions(sheet(base()), 1, combatRound(2)),
                reactions.getTotalReactions(trapped, 1, combatRound(2)));
        assertEquals(freeActions.getTotalFreeActions(sheet(base()), 1, combatRound(2)),
                freeActions.getTotalFreeActions(trapped, 1, combatRound(2)));
    }

    @Test
    void corpoMaleavelFavorsTheFirstForcaRollOfAnEvenRodada() {
        CharacterSheet plain = sheet(base());
        CharacterSheet malleable = sheet(withQuality(Quality.TENDENCIA_ATLETICA, QualityClass.MENOR));
        int before = SkillType.ATAQUE_CORPO_A_CORPO.newInteraction().applyTo(plain, combatRound(2)).getSkillRollBonus();

        assertEquals(before + Skill.ADVANTAGE_BONUS,
                SkillType.ATAQUE_CORPO_A_CORPO.newInteraction().applyTo(malleable, combatRound(2)).getSkillRollBonus());
        assertEquals(before,
                SkillType.ATAQUE_CORPO_A_CORPO.newInteraction().applyTo(malleable, combatRound(3)).getSkillRollBonus(),
                "odd Rodada");

        malleable.recordAction(new CombatantAction(SkillType.ATAQUE_CORPO_A_CORPO, AttributeDomain.STRENGTH,
                null, null, 1, null, null, Set.of()));
        assertEquals(before,
                SkillType.ATAQUE_CORPO_A_CORPO.newInteraction().applyTo(malleable, combatRound(2)).getSkillRollBonus(),
                "only the first");
    }

    // ---- Vulnerabilidade -------------------------------------------------------------------------

    private static final DamageDescriptor MAGICAL = new DamageDescriptor(DamageType.MAGICO);
    private static final DamageDescriptor PHYSICAL = new DamageDescriptor(DamageType.FISICO);

    @Test
    void dificuldadeParaReagirWorsensTheDefesaAndAddsTwoDamageOfTheChosenKind() {
        CharacterSheet plain = sheet(base());
        CharacterSheet vulnerable = sheet(withDefect(Defect.VULNERABILIDADE, DefectSeverity.LEVE, VulnerabilityKind.MAGICO));
        DefenseServiceImpl defense = new DefenseServiceImpl();
        DamageServiceImpl damage = new DamageServiceImpl();

        assertEquals(defense.getTotalDefense(plain, DefenseType.MAGIC, null, MAGICAL) + Skill.DISADVANTAGE_MALUS,
                defense.getTotalDefense(vulnerable, DefenseType.MAGIC, null, MAGICAL));
        assertEquals(defense.getTotalDefense(plain, DefenseType.PHYSICAL, null, PHYSICAL),
                defense.getTotalDefense(vulnerable, DefenseType.PHYSICAL, null, PHYSICAL));

        assertEquals(damage.calculateFinalDamage(plain, null, MAGICAL, null, 10, false) + 2,
                damage.calculateFinalDamage(vulnerable, null, MAGICAL, null, 10, false));
        assertEquals(damage.calculateFinalDamage(plain, null, PHYSICAL, null, 10, false),
                damage.calculateFinalDamage(vulnerable, null, PHYSICAL, null, 10, false));
    }

    @Test
    void anElementalVulnerabilityMatchesItsTwoElementosOnly() {
        CharacterSheet vulnerable = sheet(withDefect(Defect.VULNERABILIDADE, DefectSeverity.MODERADO,
                VulnerabilityKind.ELEMENTAL, ElementalType.FOGO, ElementalType.GELO));
        DamageServiceImpl damage = new DamageServiceImpl();
        DamageDescriptor fire = new DamageDescriptor(DamageType.ELEMENTAL, ElementalType.FOGO);
        DamageDescriptor lightning = new DamageDescriptor(DamageType.ELEMENTAL, ElementalType.ELETRICIDADE);

        assertEquals(damage.calculateFinalDamage(sheet(base()), null, fire, null, 10, false) + 3,
                damage.calculateFinalDamage(vulnerable, null, fire, null, 10, false));
        assertEquals(damage.calculateFinalDamage(sheet(base()), null, lightning, null, 10, false),
                damage.calculateFinalDamage(vulnerable, null, lightning, null, 10, false));
    }

    @Test
    void pontoFracoMakesTheDefenceOneNivelHarder() {
        CharacterSheet plain = sheet(base());
        CharacterSheet weak = sheet(withDefect(Defect.VULNERABILIDADE, DefectSeverity.MODERADO, VulnerabilityKind.MAGICO));
        EsquivaEApararInteraction esquiva = new EsquivaEApararInteraction();

        int reference = esquiva.applyTo(plain, null, null, DefenseType.MAGIC, MAGICAL).getDifficultyReduction();
        assertEquals(reference - 1, esquiva.applyTo(weak, null, null, DefenseType.MAGIC, MAGICAL).getDifficultyReduction());
        assertEquals(reference, esquiva.applyTo(weak, null, null, DefenseType.PHYSICAL, PHYSICAL).getDifficultyReduction());
    }

    @Test
    void travaMentalCannotDefendAgainstTheChosenKind() {
        CharacterSheet locked = sheet(withDefect(Defect.VULNERABILIDADE, DefectSeverity.GRAVE, VulnerabilityKind.MAGICO));
        SkillRoll greatDefence = new SkillRoll(List.of(6, 6, 5));

        assertFalse(new AttackReceiver().resolve(IncomingAttack.builder().defender(locked)
                .difficultyLevel(DifficultyLevel.EASY).defenseType(DefenseType.MAGIC).damageDescriptor(MAGICAL)
                .defenseRoll(greatDefence).build()).getDefended());
        assertTrue(new AttackReceiver().resolve(IncomingAttack.builder().defender(locked)
                .difficultyLevel(DifficultyLevel.EASY).defenseType(DefenseType.PHYSICAL).damageDescriptor(PHYSICAL)
                .defenseRoll(greatDefence).build()).getDefended());
    }

    // ---- Condições -------------------------------------------------------------------------------

    private static void tick(final CharacterSheet sheet, final int rounds) {
        for (int i = 0; i < rounds; i++) {
            sheet.tickTemporaryEffects();
        }
    }

    @Test
    void inabalavelReceivesAbaladoInsteadOfAssustadoOrApavorado() {
        CharacterSheet steady = sheet(withQuality(Quality.RESILIENCIA_HEROICA, QualityClass.MAIOR));

        steady.applyCondition(new Condition(ConditionType.APAVORADO, 3));

        Set<ConditionType> active = steady.getActiveConditions(null);
        assertTrue(active.contains(ConditionType.ABALADO));
        assertFalse(active.contains(ConditionType.APAVORADO));
        assertFalse(active.contains(ConditionType.ASSUSTADO));
    }

    @Test
    void doencaPersistenteDoublesEnvenenadoButNotCego() {
        CharacterSheet fragile = sheet(withDefect(Defect.CORPO_FRAGIL, DefectSeverity.MODERADO));

        fragile.applyCondition(new Condition(ConditionType.ENVENENADO, 2));
        fragile.applyCondition(new Condition(ConditionType.CEGO, 2));
        tick(fragile, 3);

        assertTrue(fragile.getActiveConditions(null).contains(ConditionType.ENVENENADO), "2 Rodadas became 4");
        assertFalse(fragile.getActiveConditions(null).contains(ConditionType.CEGO));
    }

    // ---- Resistência Atípica: Energia Profana/Divina ---------------------------------------------

    @Test
    void aProfanaResistanceHalvesAndAProfanaHerancaDivinaIgnoresProfaneMagias() {
        CharacterSheet resistant = sheet(withQuality(Quality.RESISTENCIA_ATIPICA, QualityClass.MENOR, EnergyKind.PROFANA));
        CharacterSheet immune = sheet(withQuality(Quality.RESISTENCIA_ATIPICA, QualityClass.MAIOR, EnergyKind.PROFANA));
        ProfanarSpell profane = ProfanarSpell.values()[0];
        VidaSpell other = VidaSpell.values()[0];
        assertTrue(profane.getTree().hasMagicType(MagicType.PROFANA));
        assertFalse(other.getTree().hasMagicType(MagicType.PROFANA));
        DamageServiceImpl damage = new DamageServiceImpl();

        assertEquals(damage.calculateFinalDamage(sheet(base()), null, DamageType.MAGICO, null, 10, true, false, profane) / 2,
                damage.calculateFinalDamage(resistant, null, DamageType.MAGICO, null, 10, true, false, profane));
        assertEquals(damage.calculateFinalDamage(sheet(base()), null, DamageType.MAGICO, null, 10, true, false, other),
                damage.calculateFinalDamage(resistant, null, DamageType.MAGICO, null, 10, true, false, other));
        assertEquals(0, damage.calculateFinalDamage(immune, null, DamageType.MAGICO, null, 10, true, false, profane));
    }

    // ---- Pronto para Ação ------------------------------------------------------------------------

    /** The relief's own bookkeeping: once per combat each, and only in combat. */
    @Test
    void prontoParaAcaoRelievesTheFirstActivationOfEachCombatOnly() {
        CharacterSheet ready = sheet(withQuality(Quality.CENTELHA_MAIOR, QualityClass.MENOR));
        QualidadeFeat relief = QualidadeFeat.CENTELHA_MAIOR_MENOR;

        assertFalse(relief.waivesTitleActivationDeterminationCost(ready, null), "outside a Cena de Combate");
        assertTrue(relief.waivesTitleActivationDeterminationCost(ready, combatRound(1)));
        assertTrue(relief.capsTitleActivationEgoCost(ready, combatRound(1)));

        relief.onTitleActivationCostRelief(ready, true, false);
        assertFalse(relief.waivesTitleActivationDeterminationCost(ready, combatRound(1)));
        assertTrue(relief.capsTitleActivationEgoCost(ready, combatRound(1)), "the Ego relief is its own");

        ready.endCombat();
        assertTrue(relief.waivesTitleActivationDeterminationCost(ready, combatRound(1)), "a new combat");
    }
}
