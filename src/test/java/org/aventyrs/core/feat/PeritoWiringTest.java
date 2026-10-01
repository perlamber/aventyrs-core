package org.aventyrs.core.feat;

import org.aventyrs.core.action.ActionPointsService;
import org.aventyrs.core.action.ActionPointsServiceImpl;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.DamageBonus;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.ActiveAbilityService;
import org.aventyrs.core.character.services.ActiveAbilityServiceImpl;
import org.aventyrs.core.character.services.FeatService;
import org.aventyrs.core.character.services.FeatServiceImpl;
import org.aventyrs.core.character.services.FreeActionsService;
import org.aventyrs.core.character.services.FreeActionsServiceImpl;
import org.aventyrs.core.character.services.ReactionsService;
import org.aventyrs.core.character.services.ReactionsServiceImpl;
import org.aventyrs.core.effect.EffectChainService;
import org.aventyrs.core.effect.EffectChainServiceImpl;
import org.aventyrs.core.scene.InitiativePosition;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.scene.Scene;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.ActionOutcome;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantAction;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.Hidden;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.CriticalResult;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillGraduation;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.skill.SkillTraitKind;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.skill.atletismo.AtletismoCompetencyAbility;
import org.aventyrs.core.skill.atletismo.AtletismoSpecialization;
import org.aventyrs.core.skill.attention.AttentionSpecialization;
import org.aventyrs.core.title.santo.Santo;
import org.aventyrs.core.character.TitleSlot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.aventyrs.core.util.TranslatableMessages.DEFER_TO_LAST_NOT_PERMITTED;
import static org.aventyrs.core.util.TranslatableMessages.FEAT_ACTIVATION_NOT_PERMITTED;
import static org.aventyrs.core.util.TranslatableMessages.FEAT_PREREQUISITE_NOT_MET;
import static org.aventyrs.core.util.TranslatableMessages.FEAT_REQUIRES_CHOICE;
import static org.aventyrs.core.util.TranslatableMessages.REROLL_NOT_GRANTED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every {@link PeritoFeat} constant, read off the service a caller reads it from — a Perícia roll's
 * bonus and GD, its price, the Scene's order of play, a Corrente's threshold, an activated state —
 * on a character who acquired the Talento through {@link FeatService#grantFeat}.
 */
class PeritoWiringTest {

    /** 9 — no critical, no Falha. */
    private static final List<Integer> PLAIN = List.of(3, 3, 3);

    private final FeatService featService = new FeatServiceImpl();
    private final ActionPointsService actionPointsService = new ActionPointsServiceImpl();
    private final ReactionsService reactionsService = new ReactionsServiceImpl();
    private final FreeActionsService freeActionsService = new FreeActionsServiceImpl();
    private final ActiveAbilityService activeAbilityService = new ActiveAbilityServiceImpl();
    private final EffectChainService effectChainService = new EffectChainServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    // ---------- fixtures ----------

    private static CharacterSkill trained(final SkillType skill, final int graduation) {
        return trained(skill, graduation, List.of());
    }

    private static CharacterSkill trained(final SkillType skill, final int graduation,
                                          final List<org.aventyrs.core.skill.SkillSpecialization> specializations) {
        return CharacterSkill.builder()
                .skill(skill.newSkillInstance())
                .graduation(SkillGraduation.builder().graduationValue(graduation).build())
                .specializations(specializations)
                .build();
    }

    /** Gnose and Foco 5, Atletismo 5, Persuasão/Conhecimentos 4 — enough for every Perito Pré-requisito below. */
    private static Character.CharacterBuilder perito() {
        return CharacterFixture.blank(CharacterFixture.BLANK)
                .id(UUID.randomUUID())
                .attributes(CharacterAttributes.of(Map.of(AttributeDomain.GNOSE, 5, AttributeDomain.FOCUS, 5,
                        AttributeDomain.INSTINCT, 3)))
                .skill(SkillType.ATLETISMO, trained(SkillType.ATLETISMO, 5))
                .skill(SkillType.PERSUASAO, trained(SkillType.PERSUASAO, 4))
                .skill(SkillType.CONHECIMENTOS, trained(SkillType.CONHECIMENTOS, 4))
                .feats(new ArrayList<>());
    }

    /** A funded sheet for character, every feat granted through {@link FeatService#grantFeat}. */
    private CharacterSheet acquire(final Character character, final Feat... feats) throws IllegalOperationException {
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        sheet.accumulateExperience(BigDecimal.valueOf(200));
        for (Feat feat : feats) {
            featService.grantFeat(character, sheet, feat);
        }
        return sheet;
    }

    private static CharacterSheet bystander(final SkillType trainedIn) {
        Character.CharacterBuilder builder = CharacterFixture.blank(CharacterFixture.BLANK).id(UUID.randomUUID())
                .feats(new ArrayList<>());
        if (trainedIn != null) {
            builder.skill(trainedIn, trained(trainedIn, 1));
        }
        return CharacterSheet.of(builder.build(), new Player());
    }

    private static InteractionResult roll(final CombatantSheet sheet, final SkillType skill, final SceneContext context,
                                          final SkillRoll roll) {
        return skill.newInteraction().applyTo(sheet, context, roll);
    }

    private static SkillRoll activating(final Feat feat, final ActionCost cost) {
        return new SkillRoll(PLAIN, null, null, cost, null, Set.of(feat));
    }

    private static SceneContext combatRound(final int round) {
        return new SceneContext(List.of(), List.of(), Map.of(), null, true, round, false);
    }

    private static CombatantAction failed(final SkillType skill, final Set<Feat> activated) {
        return new CombatantAction(skill, AttributeDomain.STRENGTH, null, ActionCost.ofActionPoints(2), 0,
                new ActionOutcome(false, -3, CriticalResult.NONE, null), null, activated);
    }

    // ---------- Foco em Perícia ----------

    @Test
    void focoEmPericiaOffersOnlyTrainedPericiasAndAttackOnesAtFourGraduacoes() {
        Character character = perito()
                .skill(SkillType.ATAQUE_CORPO_A_CORPO, trained(SkillType.ATAQUE_CORPO_A_CORPO, 3))
                .skill(SkillType.ATAQUE_A_DISTANCIA, trained(SkillType.ATAQUE_A_DISTANCIA, 4))
                .build();

        List<?> options = PeritoFeat.FOCO_EM_PERICIA.resolveRequiredChoices(character).get(0).options();

        assertTrue(options.contains(SkillType.ATLETISMO));
        assertTrue(options.contains(SkillType.ATAQUE_A_DISTANCIA));
        assertFalse(options.contains(SkillType.ATAQUE_CORPO_A_CORPO), "3 Graduações de ataque are not enough");
        assertFalse(options.contains(SkillType.FURTIVIDADE), "untrained");
    }

    @Test
    void focoEmPericiaRefusesAnUntrainedChoice() {
        Character character = perito().build();

        IllegalOperationException error = assertThrows(IllegalOperationException.class,
                () -> acquire(character, FocoEmPericiaFeat.of(SkillType.FURTIVIDADE)));
        assertEquals(FEAT_PREREQUISITE_NOT_MET, error.getMessage());
    }

    // ---------- Discreto / Exibicionista (the neutral allegiance) ----------

    /** A Scene with the holder, and one sheet per extra group — hostile unless neutralGroups says so. */
    private static SceneContext sceneAround(final CombatantSheet holder, final Map<CombatantSheet, Range> distances,
                                            final List<CombatantSheet> allies, final List<CombatantSheet> hostiles,
                                            final List<CombatantSheet> neutrals) {
        Scene scene = new Scene();
        UUID party = UUID.randomUUID();
        UUID foes = UUID.randomUUID();
        UUID crowd = UUID.randomUUID();
        scene.addParticipant(holder, 10, party);
        allies.forEach(ally -> scene.addParticipant(ally, 5, party));
        hostiles.forEach(hostile -> scene.addParticipant(hostile, 5, foes));
        neutrals.forEach(neutral -> scene.addParticipant(neutral, 5, crowd));
        scene.declareNeutral(party, crowd);
        return scene.buildContext(holder, distances);
    }

    @Test
    void discretoAddsAnotherVantagemOnlyWithNoAllyOrNeutralInCurta() throws IllegalOperationException {
        Character character = perito().build();
        CharacterSheet sheet = acquire(character, FocoEmPericiaFeat.of(SkillType.ATLETISMO));
        CharacterSheet foe = bystander(null);
        SceneContext alone = sceneAround(sheet, Map.of(foe, Range.ADJACENTE), List.of(), List.of(foe), List.of());
        int focusOnly = roll(sheet, SkillType.ATLETISMO, alone, new SkillRoll(PLAIN)).getSkillRollBonus();

        featService.grantFeat(character, sheet, PeritoFeat.DISCRETO);

        assertEquals(focusOnly + Skill.ADVANTAGE_BONUS,
                roll(sheet, SkillType.ATLETISMO, alone, new SkillRoll(PLAIN)).getSkillRollBonus(),
                "an enemy nearby does not spoil it");
        CharacterSheet ally = bystander(null);
        SceneContext withAlly = sceneAround(sheet, Map.of(ally, Range.DISTANCIA_CURTA), List.of(ally), List.of(),
                List.of());
        assertEquals(focusOnly, roll(sheet, SkillType.ATLETISMO, withAlly, new SkillRoll(PLAIN)).getSkillRollBonus());
        CharacterSheet onlooker = bystander(null);
        SceneContext withNeutral = sceneAround(sheet, Map.of(onlooker, Range.DISTANCIA_CURTA), List.of(), List.of(),
                List.of(onlooker));
        assertEquals(focusOnly,
                roll(sheet, SkillType.ATLETISMO, withNeutral, new SkillRoll(PLAIN)).getSkillRollBonus());
        assertEquals(focusOnly, roll(sheet, SkillType.ATLETISMO, null, new SkillRoll(PLAIN)).getSkillRollBonus(),
                "with no Scene nobody can tell who is near");
    }

    @Test
    void discretoGrantsVantagemEmDanoWhileHiddenFromEveryEnemy() throws IllegalOperationException {
        Character character = perito()
                .skill(SkillType.ATAQUE_CORPO_A_CORPO, trained(SkillType.ATAQUE_CORPO_A_CORPO, 4))
                .build();
        CharacterSheet sheet = acquire(character, FocoEmPericiaFeat.of(SkillType.ATLETISMO), PeritoFeat.DISCRETO);
        CharacterSheet foe = bystander(null);
        SceneContext alone = sceneAround(sheet, Map.of(foe, Range.DISTANCIA_MEDIA), List.of(), List.of(foe), List.of());
        int seen = danoOf(roll(sheet, SkillType.ATAQUE_CORPO_A_CORPO, alone, null));

        Hidden hidden = Hidden.fromRoll(20, false);
        sheet.applyCondition(hidden);
        assertEquals(seen + Skill.ADVANTAGE_BONUS, danoOf(roll(sheet, SkillType.ATAQUE_CORPO_A_CORPO, alone, null)));

        hidden.markDetectedBy(foe);
        assertEquals(seen, danoOf(roll(sheet, SkillType.ATAQUE_CORPO_A_CORPO, alone, null)),
                "an enemy who sees through it sees the holder");
    }

    private static int danoOf(final InteractionResult result) {
        DamageBonus bonus = result.getDamageBonus();
        return bonus == null ? 0 : bonus.getValue();
    }

    @Test
    void exibicionistaNeedsFourNeutralCharactersInTheScene() throws IllegalOperationException {
        Character character = perito().build();
        CharacterSheet sheet = acquire(character, FocoEmPericiaFeat.of(SkillType.PERSUASAO));
        List<CombatantSheet> four = List.of(bystander(null), bystander(null), bystander(null), bystander(null));
        SceneContext crowd = sceneAround(sheet, Map.of(), List.of(), List.of(), four);
        SceneContext thin = sceneAround(sheet, Map.of(), List.of(), List.of(), four.subList(0, 3));
        int before = roll(sheet, SkillType.PERSUASAO, crowd, new SkillRoll(PLAIN)).getSkillRollBonus();

        featService.grantFeat(character, sheet, PeritoFeat.EXIBICIONISTA);

        assertEquals(before + Skill.ADVANTAGE_BONUS,
                roll(sheet, SkillType.PERSUASAO, crowd, new SkillRoll(PLAIN)).getSkillRollBonus());
        assertEquals(before, roll(sheet, SkillType.PERSUASAO, thin, new SkillRoll(PLAIN)).getSkillRollBonus());
    }

    /** "Personagens inteligentes" (core 0.0.84): a bystander not intelligent is no audience. */
    @Test
    void exibicionistaCountsOnlyIntelligentBystanders() throws IllegalOperationException {
        Character character = perito().build();
        CharacterSheet sheet = acquire(character, FocoEmPericiaFeat.of(SkillType.PERSUASAO));
        CharacterSheet herd = bystander(null);
        herd.setIntelligent(false);
        SceneContext crowd = sceneAround(sheet, Map.of(), List.of(), List.of(),
                List.of(bystander(null), bystander(null), bystander(null), herd));
        int before = roll(sheet, SkillType.PERSUASAO, crowd, new SkillRoll(PLAIN)).getSkillRollBonus();

        featService.grantFeat(character, sheet, PeritoFeat.EXIBICIONISTA);

        assertEquals(before, roll(sheet, SkillType.PERSUASAO, crowd, new SkillRoll(PLAIN)).getSkillRollBonus());
    }

    @Test
    void theAggressionMapIsDirectionalButHostilityIsNot() {
        Scene scene = new Scene();
        UUID party = UUID.randomUUID();
        UUID wolves = UUID.randomUUID();
        UUID crowd = UUID.randomUUID();
        CharacterSheet hero = bystander(null);
        CharacterSheet wolf = bystander(null);
        CharacterSheet townsman = bystander(null);
        scene.addParticipant(hero, 10, party);
        scene.addParticipant(wolf, 5, wolves);
        scene.addParticipant(townsman, 5, crowd);

        scene.setAggressive(party, wolves, false);           // the party would rather pass by
        scene.declareNeutral(party, crowd);

        assertEquals(List.of(wolf), scene.getEnemies(hero), "the wolves are still aggressive towards the party");
        assertEquals(List.of(townsman), scene.getNeutrals(hero));
        assertEquals(List.of(townsman), scene.buildContext(hero, Map.of()).getNeutrals());
    }

    // ---------- Perito Veloz ----------

    @Test
    void peritoVelozTakesAPaOffTheFirstRollOfTheFocoPericiaEachTurn() throws IllegalOperationException {
        Character character = perito().build();
        CharacterSheet sheet = acquire(character, FocoEmPericiaFeat.of(SkillType.ATLETISMO), PeritoFeat.PERITO_VELOZ);
        sheet.startTurn(1);

        assertEquals(ActionCost.ofActionPoints(1),
                actionPointsService.getSkillRollCost(sheet, SkillType.ATLETISMO, Set.of(), 1, null));
        assertEquals(ActionCost.ofActionPoints(2),
                actionPointsService.getSkillRollCost(sheet, SkillType.PERSUASAO, Set.of(), 1, null));

        sheet.recordAction(new CombatantAction(SkillType.ATLETISMO, AttributeDomain.STRENGTH, null,
                ActionCost.ofActionPoints(1), 1, null));
        assertEquals(ActionCost.ofActionPoints(2),
                actionPointsService.getSkillRollCost(sheet, SkillType.ATLETISMO, Set.of(), 1, null));
    }

    @Test
    void peritoVelozReachesAnAttackWhenTheFocoIsAPericiaDeAtaque() throws IllegalOperationException {
        Character character = perito()
                .skill(SkillType.ATAQUE_CORPO_A_CORPO, trained(SkillType.ATAQUE_CORPO_A_CORPO, 4))
                .build();
        CharacterSheet sheet = acquire(character, FocoEmPericiaFeat.of(SkillType.ATAQUE_CORPO_A_CORPO),
                PeritoFeat.PERITO_VELOZ);
        sheet.startTurn(1);

        assertEquals(ActionCost.ofActionPoints(1), actionPointsService.getAttackCost(sheet,
                SkillType.ATAQUE_CORPO_A_CORPO, null, Set.of(), 1));
    }

    // ---------- Mestre Perito ----------

    @Test
    void mestrePeritoNeedsFiveGraduacoesInTheFocoPericia() throws IllegalOperationException {
        Character character = perito().build();
        CharacterSheet sheet = acquire(character, FocoEmPericiaFeat.of(SkillType.PERSUASAO), PeritoFeat.PERITO_VELOZ);

        assertThrows(IllegalOperationException.class,
                () -> featService.grantFeat(character, sheet, PeritoFeat.MESTRE_PERITO));
    }

    @Test
    void mestrePeritoRollsTheFocoAsAFreeActionOrReactionInTheFirstRodadasOncePerRodada()
            throws IllegalOperationException {
        Character character = perito().build();
        CharacterSheet sheet = acquire(character, FocoEmPericiaFeat.of(SkillType.ATLETISMO), PeritoFeat.PERITO_VELOZ,
                PeritoFeat.MESTRE_PERITO);
        Feat mestre = PeritoFeat.MESTRE_PERITO;

        roll(sheet, SkillType.ATLETISMO, combatRound(1), activating(mestre, ActionCost.REACTION));
        roll(sheet, SkillType.ATLETISMO, combatRound(2), activating(mestre, ActionCost.FREE_ACTION));
        assertRefused(sheet, SkillType.ATLETISMO, combatRound(2), activating(mestre, ActionCost.REACTION));
        assertRefused(sheet, SkillType.ATLETISMO, combatRound(3), activating(mestre, ActionCost.FREE_ACTION));
        assertRefused(sheet, SkillType.PERSUASAO, combatRound(1), activating(mestre, ActionCost.FREE_ACTION));
        assertRefused(sheet, SkillType.ATLETISMO, null, activating(mestre, ActionCost.FREE_ACTION));

        sheet.recordAction(new CombatantAction(SkillType.ATLETISMO, AttributeDomain.STRENGTH, null,
                ActionCost.FREE_ACTION, 1, null, null, Set.of(mestre)));
        assertRefused(sheet, SkillType.ATLETISMO, combatRound(1), activating(mestre, ActionCost.FREE_ACTION));
    }

    private static void assertRefused(final CombatantSheet sheet, final SkillType skill, final SceneContext context,
                                      final SkillRoll roll) {
        IllegalOperationException error = assertThrows(IllegalOperationException.class,
                () -> roll(sheet, skill, context, roll));
        assertEquals(FEAT_ACTIVATION_NOT_PERMITTED, error.getMessage());
    }

    // ---------- Maestria em Perícia ----------

    private Character maestro() {
        return perito()
                .skillCompetencyAbility(AtletismoCompetencyAbility.ANFIBIO)
                .skillCompetencyAbility(AtletismoCompetencyAbility.ALPINISTA_VELOZ)
                .build();
    }

    @Test
    void maestriaNeedsTwoHabilidadesOrFourEspecializacoesOfTheFoco() throws IllegalOperationException {
        Character character = perito().build();
        CharacterSheet sheet = acquire(character, FocoEmPericiaFeat.of(SkillType.ATLETISMO));

        assertThrows(IllegalOperationException.class,
                () -> featService.grantFeat(character, sheet, PeritoFeat.MAESTRIA_EM_PERICIA));
        Character maestro = maestro();
        acquire(maestro, FocoEmPericiaFeat.of(SkillType.ATLETISMO), PeritoFeat.MAESTRIA_EM_PERICIA);
    }

    @Test
    void maestriaOutOfCombatEasesTheGdTwoNiveisAtTripleThePrice() throws IllegalOperationException {
        Character character = maestro();
        CharacterSheet sheet = acquire(character, FocoEmPericiaFeat.of(SkillType.ATLETISMO),
                PeritoFeat.MAESTRIA_EM_PERICIA);
        Feat maestria = PeritoFeat.MAESTRIA_EM_PERICIA;
        int plain = roll(sheet, SkillType.ATLETISMO, null, new SkillRoll(PLAIN)).getDifficultyReduction();

        assertEquals(plain + 2,
                roll(sheet, SkillType.ATLETISMO, null, activating(maestria, null)).getDifficultyReduction());
        assertEquals(ActionCost.ofActionPoints(6),
                actionPointsService.getSkillRollCost(sheet, SkillType.ATLETISMO, Set.of(maestria), 0, null));
        IllegalOperationException error = assertThrows(IllegalOperationException.class,
                () -> roll(sheet, SkillType.ATLETISMO, null, activating(maestria, null).rerollingLowestDie(6)));
        assertEquals(REROLL_NOT_GRANTED, error.getMessage(), "out of combat the reroll is not on offer");
    }

    @Test
    void maestriaInCombatRerollsTheLowestDieInstead() throws IllegalOperationException {
        Character character = maestro();
        CharacterSheet sheet = acquire(character, FocoEmPericiaFeat.of(SkillType.ATLETISMO),
                PeritoFeat.MAESTRIA_EM_PERICIA);
        Feat maestria = PeritoFeat.MAESTRIA_EM_PERICIA;
        SceneContext combat = combatRound(1);
        int plain = roll(sheet, SkillType.ATLETISMO, combat, new SkillRoll(PLAIN)).getDifficultyReduction();

        InteractionResult rerolled = roll(sheet, SkillType.ATLETISMO, combat,
                activating(maestria, null).rerollingLowestDie(6));

        assertEquals(plain, rerolled.getDifficultyReduction());
        assertEquals(ActionCost.ofActionPoints(2),
                actionPointsService.getSkillRollCost(sheet, SkillType.ATLETISMO, Set.of(maestria), 1, combat));
    }

    @Test
    void maestriaCannotBeUsedWithPersuasao() throws IllegalOperationException {
        Character character = perito()
                .skillCompetencyAbility(org.aventyrs.core.skill.persuasao.PersuasaoCompetencyAbility.values()[0])
                .skillCompetencyAbility(org.aventyrs.core.skill.persuasao.PersuasaoCompetencyAbility.values()[1])
                .build();
        CharacterSheet sheet = acquire(character, FocoEmPericiaFeat.of(SkillType.PERSUASAO),
                PeritoFeat.MAESTRIA_EM_PERICIA);

        assertRefused(sheet, SkillType.PERSUASAO, null, activating(PeritoFeat.MAESTRIA_EM_PERICIA, null));
    }

    // ---------- Controle da Situação ----------

    @Test
    void controleDaSituacaoNeedsFourGraduacoesInThreePericias() {
        Character narrow = perito().skill(SkillType.CONHECIMENTOS, trained(SkillType.CONHECIMENTOS, 3)).build();
        Character broad = perito().build();

        assertFalse(PeritoFeat.CONTROLE_DA_SITUACAO.isEligible(narrow));
        assertTrue(PeritoFeat.CONTROLE_DA_SITUACAO.isEligible(broad));
    }

    // ---------- Lembrar Como se Faz / Lembrar, Revisar e Aprimorar ----------

    @Test
    void lembrarComoSeFazRetriesAFailedRollForOnePaWithVantagem() throws IllegalOperationException {
        Character character = perito().build();
        CharacterSheet sheet = acquire(character, PeritoFeat.LEMBRAR_COMO_SE_FAZ);
        Feat lembrar = PeritoFeat.LEMBRAR_COMO_SE_FAZ;
        int plain = roll(sheet, SkillType.ATLETISMO, null, new SkillRoll(PLAIN)).getSkillRollBonus();

        assertRefused(sheet, SkillType.ATLETISMO, null, activating(lembrar, null));
        sheet.recordAction(failed(SkillType.ATLETISMO, Set.of()));

        assertEquals(plain + Skill.ADVANTAGE_BONUS,
                roll(sheet, SkillType.ATLETISMO, null, activating(lembrar, null)).getSkillRollBonus());
        assertEquals(ActionCost.ofActionPoints(1),
                actionPointsService.getSkillRollCost(sheet, SkillType.ATLETISMO, Set.of(lembrar), 0, null));
        assertRefused(sheet, SkillType.PERSUASAO, null, activating(lembrar, null));
    }

    @Test
    void lembrarComoSeFazCannotRetryARetryOrASuccess() throws IllegalOperationException {
        Character character = perito().build();
        CharacterSheet sheet = acquire(character, PeritoFeat.LEMBRAR_COMO_SE_FAZ);
        Feat lembrar = PeritoFeat.LEMBRAR_COMO_SE_FAZ;

        sheet.recordAction(failed(SkillType.ATLETISMO, Set.of(lembrar)));
        assertRefused(sheet, SkillType.ATLETISMO, null, activating(lembrar, null));

        sheet.recordAction(new CombatantAction(SkillType.ATLETISMO, AttributeDomain.STRENGTH, null,
                ActionCost.ofActionPoints(2), 0, new ActionOutcome(true, 2, CriticalResult.NONE, null)));
        assertRefused(sheet, SkillType.ATLETISMO, null, activating(lembrar, null));
    }

    @Test
    void lembrarRevisarEAprimorarEasesTheRetryOneNivel() throws IllegalOperationException {
        Character character = perito().build();
        CharacterSheet sheet = acquire(character, PeritoFeat.LEMBRAR_COMO_SE_FAZ);
        Feat lembrar = PeritoFeat.LEMBRAR_COMO_SE_FAZ;
        sheet.recordAction(failed(SkillType.ATLETISMO, Set.of()));
        int retry = roll(sheet, SkillType.ATLETISMO, null, activating(lembrar, null)).getDifficultyReduction();

        featService.grantFeat(character, sheet, PeritoFeat.LEMBRAR_REVISAR_E_APRIMORAR);

        assertEquals(retry + 1,
                roll(sheet, SkillType.ATLETISMO, null, activating(lembrar, null)).getDifficultyReduction());
        assertEquals(roll(sheet, SkillType.ATLETISMO, null, new SkillRoll(PLAIN)).getDifficultyReduction() + 1,
                retry + 1, "only the retry is eased");
    }

    // ---------- Leitura Comportamental ----------

    @Test
    void leituraComportamentalEasesDiscernirMotivacaoOnly() throws IllegalOperationException {
        Character character = perito()
                .skill(SkillType.ATTENTION, trained(SkillType.ATTENTION, 2,
                        List.of(AttentionSpecialization.DISCERNIR_MOTIVACAO)))
                .build();
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        SkillRoll discernir = new SkillRoll(PLAIN, AttentionSpecialization.DISCERNIR_MOTIVACAO);
        int before = roll(sheet, SkillType.ATTENTION, null, discernir).getDifficultyReduction();
        int plainBefore = roll(sheet, SkillType.ATTENTION, null, new SkillRoll(PLAIN)).getDifficultyReduction();

        acquire(character, PeritoFeat.LEITURA_COMPORTAMENTAL);

        assertEquals(before + 1, roll(sheet, SkillType.ATTENTION, null, discernir).getDifficultyReduction());
        assertEquals(plainBefore, roll(sheet, SkillType.ATTENTION, null, new SkillRoll(PLAIN)).getDifficultyReduction());
    }

    // ---------- Mestre em Atuação ----------

    @Test
    void mestreEmAtuacaoNeedsTwoGraduacoesInArtesToo() {
        Character character = perito().build();

        assertThrows(IllegalOperationException.class, () -> acquire(character, PeritoFeat.MESTRE_EM_ATUACAO));
    }

    @Test
    void mestreEmAtuacaoGrantsVantagemOnlyOnARollDeclaredAsAPerformance() throws IllegalOperationException {
        Character character = perito().skill(SkillType.ARTES, trained(SkillType.ARTES, 2)).build();
        CharacterSheet sheet = acquire(character, PeritoFeat.MESTRE_EM_ATUACAO);
        Feat atuacao = PeritoFeat.MESTRE_EM_ATUACAO;
        int plain = roll(sheet, SkillType.ARTES, null, new SkillRoll(PLAIN)).getSkillRollBonus();

        assertEquals(plain + Skill.ADVANTAGE_BONUS,
                roll(sheet, SkillType.ARTES, null, activating(atuacao, null)).getSkillRollBonus());
        assertRefused(sheet, SkillType.ATLETISMO, null, activating(atuacao, null));
    }

    // ---------- Treinado em Perícias ----------

    @Test
    void treinadoEmPericiasIsOpenAtCreationAndGatedOnGraduacoesAfterwards() {
        Character fresh = CharacterFixture.blank(CharacterFixture.BLANK).feats(new ArrayList<>())
                .skill(SkillType.ATLETISMO, trained(SkillType.ATLETISMO, 1))
                .skill(SkillType.PERSUASAO, trained(SkillType.PERSUASAO, 1))
                .skill(SkillType.ARTES, trained(SkillType.ARTES, 1))
                .build();

        assertTrue(PeritoFeat.TREINADO_EM_PERICIAS.isEligibleAtCreation(fresh, null));
        assertFalse(PeritoFeat.TREINADO_EM_PERICIAS.isEligible(fresh));
        assertTrue(PeritoFeat.TREINADO_EM_PERICIAS.isEligible(perito().build()));
    }

    @Test
    void treinadoEmPericiasDeclaresThreeTrainedPericiasOwingEitherTrait() {
        Character character = perito().build();

        FeatChoice<?> choice = PeritoFeat.TREINADO_EM_PERICIAS.resolveRequiredChoices(character).get(0);

        assertEquals(SkillType.class, choice.type());
        assertEquals(3, choice.picks());
        assertEquals(List.of(SkillType.ATLETISMO, SkillType.PERSUASAO, SkillType.CONHECIMENTOS), choice.options());
        assertEquals(Set.of(SkillTraitKind.SPECIALIZATION, SkillTraitKind.COMPETENCY_ABILITY),
                PeritoFeat.TREINADO_EM_PERICIAS.resolveRequiredSkillTraitKinds());
        IllegalOperationException error = assertThrows(IllegalOperationException.class,
                () -> acquire(character, PeritoFeat.TREINADO_EM_PERICIAS));
        assertEquals(FEAT_REQUIRES_CHOICE, error.getMessage());
    }

    // ---------- Analista Tático / Grande Analista Tático ----------

    private Character analyst() {
        return perito().build();
    }

    @Test
    void analistaTaticoDefersToLastAndGainsAFreeActionAndAReactionThere() throws IllegalOperationException {
        Character character = analyst();
        CharacterSheet sheet = acquire(character, PeritoFeat.ANALISTA_TATICO);
        CharacterSheet slow = bystander(null);
        Scene scene = new Scene();
        scene.addParticipant(sheet, 20);
        scene.addParticipant(slow, 5);
        scene.startCombat();
        SceneContext first = scene.buildContext(sheet, Map.of());
        int reactions = reactionsService.getTotalReactions(sheet, 1, first);
        int freeActions = freeActionsService.getTotalFreeActions(sheet, 1, first);

        List<CombatantSheet> order = scene.deferToLast(sheet);

        assertEquals(List.of(slow, sheet), order);
        SceneContext last = scene.buildContext(sheet, Map.of());
        assertEquals(InitiativePosition.LAST, last.getInitiativePosition());
        assertEquals(reactions + 1, reactionsService.getTotalReactions(sheet, 1, last));
        assertEquals(freeActions + 1, freeActionsService.getTotalFreeActions(sheet, 1, last));
        scene.next();
        scene.next();
        scene.next();                                       // a Rodada wraps and re-sorts
        assertEquals(List.of(slow, sheet), scene.getParticipantsInInitiativeOrder(), "it stays last");
    }

    @Test
    void deferringToLastNeedsTheTalentoAndComesBeforeAnyAction() throws IllegalOperationException {
        CharacterSheet plain = bystander(null);
        CharacterSheet analyst = acquire(analyst(), PeritoFeat.ANALISTA_TATICO);
        Scene scene = new Scene();
        scene.addParticipant(plain, 20);
        scene.addParticipant(analyst, 15);
        scene.addParticipant(bystander(null), 5);
        scene.startCombat();

        assertEquals(DEFER_TO_LAST_NOT_PERMITTED,
                assertThrows(IllegalOperationException.class, () -> scene.deferToLast(plain)).getMessage());
        analyst.recordAction(new CombatantAction(SkillType.ATLETISMO, AttributeDomain.STRENGTH, null,
                ActionCost.ofActionPoints(2), 0, null));
        assertEquals(DEFER_TO_LAST_NOT_PERMITTED,
                assertThrows(IllegalOperationException.class, () -> scene.deferToLast(analyst)).getMessage());
    }

    @Test
    void grandeAnalistaTaticoWidensTheMargemCriticaPerTituloWhileLast() throws IllegalOperationException {
        Character character = analyst();
        character.grantTitle(new Santo(List.of(), List.of()), TitleSlot.PRIMARY);
        CharacterSheet sheet = acquire(character, PeritoFeat.GRANDE_ANALISTA_TATICO);
        SceneContext last = new SceneContext(List.of(), List.of(), Map.of(), null, true, 1, false, null, null,
                null, InitiativePosition.LAST);
        SceneContext first = new SceneContext(List.of(), List.of(), Map.of(), null, true, 1, false, null, null,
                null, InitiativePosition.FIRST);

        assertEquals(CriticalResult.NONE,
                roll(sheet, SkillType.ATLETISMO, first, new SkillRoll(List.of(6, 6, 4))).getCriticalResult());
        assertEquals(CriticalResult.ACERTO_CRITICO_MENOR,
                roll(sheet, SkillType.ATLETISMO, last, new SkillRoll(List.of(6, 6, 4))).getCriticalResult());
    }

    @Test
    void grandeAnalistaTaticoMovesTheCorrenteThresholdBothWaysWhileLast() throws IllegalOperationException {
        Character character = analyst();
        character.grantTitle(new Santo(List.of(), List.of()), TitleSlot.PRIMARY);
        CharacterSheet analyst = acquire(character, PeritoFeat.GRANDE_ANALISTA_TATICO);
        CharacterSheet other = bystander(null);
        int base = EffectChainService.BASE_REQUIRED_MARGIN;

        assertEquals(base + 1, effectChainService.getRequiredMargin(other, InitiativePosition.FIRST,
                analyst, InitiativePosition.LAST), "harder to chain onto them");
        assertEquals(base - 1, effectChainService.getRequiredMargin(analyst, InitiativePosition.LAST,
                other, InitiativePosition.FIRST), "easier for them to chain");
        assertEquals(base, effectChainService.getRequiredMargin(analyst, InitiativePosition.MIDDLE,
                other, InitiativePosition.FIRST));
    }

    // ---------- Trabalho em Equipe / Aprimorado ----------

    @Test
    void trabalhoEmEquipeNeedsTwoAlliesInCurtaTrainedInTheFoco() throws IllegalOperationException {
        Character character = perito().build();
        CharacterSheet sheet = acquire(character, FocoEmPericiaFeat.of(SkillType.ATLETISMO));
        CharacterSheet trainedAlly = bystander(SkillType.ATLETISMO);
        CharacterSheet otherTrainedAlly = bystander(SkillType.ATLETISMO);
        CharacterSheet untrainedAlly = bystander(null);
        Map<CombatantSheet, Range> near = new HashMap<>();
        near.put(trainedAlly, Range.DISTANCIA_CURTA);
        near.put(otherTrainedAlly, Range.ADJACENTE);
        near.put(untrainedAlly, Range.ADJACENTE);
        SceneContext backed = sceneAround(sheet, near, List.of(trainedAlly, otherTrainedAlly), List.of(), List.of());
        SceneContext thin = sceneAround(sheet, near, List.of(trainedAlly, untrainedAlly), List.of(), List.of());
        int before = roll(sheet, SkillType.ATLETISMO, backed, new SkillRoll(PLAIN)).getSkillRollBonus();

        featService.grantFeat(character, sheet, PeritoFeat.TRABALHO_EM_EQUIPE);

        assertEquals(before + Skill.ADVANTAGE_BONUS,
                roll(sheet, SkillType.ATLETISMO, backed, new SkillRoll(PLAIN)).getSkillRollBonus());
        assertEquals(before, roll(sheet, SkillType.ATLETISMO, thin, new SkillRoll(PLAIN)).getSkillRollBonus());
    }

    @Test
    void trabalhoEmEquipeAprimoradoTradesTheVantagemForANivel() throws IllegalOperationException {
        Character character = perito().build();
        character.grantTitle(new Santo(List.of(), List.of()), TitleSlot.PRIMARY);
        CharacterSheet sheet = acquire(character, FocoEmPericiaFeat.of(SkillType.ATLETISMO),
                PeritoFeat.TRABALHO_EM_EQUIPE);
        CharacterSheet a = bystander(SkillType.ATLETISMO);
        CharacterSheet b = bystander(SkillType.ATLETISMO);
        SceneContext backed = sceneAround(sheet, Map.of(a, Range.ADJACENTE, b, Range.ADJACENTE), List.of(a, b),
                List.of(), List.of());
        InteractionResult before = roll(sheet, SkillType.ATLETISMO, backed, new SkillRoll(PLAIN));

        featService.grantFeat(character, sheet, PeritoFeat.TRABALHO_EM_EQUIPE_APRIMORADO);
        InteractionResult after = roll(sheet, SkillType.ATLETISMO, backed, new SkillRoll(PLAIN));

        assertEquals(before.getSkillRollBonus() - Skill.ADVANTAGE_BONUS, after.getSkillRollBonus());
        assertEquals(before.getDifficultyReduction() + 1, after.getDifficultyReduction());
    }

    // ---------- Criança do Mar / Rei da Montanha ----------

    @Test
    void criancaDoMarBuysARodadaOfBreathingUnderwaterForOnePd() throws IllegalOperationException {
        Character character = perito()
                .skill(SkillType.ATLETISMO, trained(SkillType.ATLETISMO, 5, List.of(AtletismoSpecialization.PULMAO_DE_ACO)))
                .skillCompetencyAbility(AtletismoCompetencyAbility.ANFIBIO)
                .build();
        CharacterSheet sheet = acquire(character, PeritoFeat.CRIANCA_DO_MAR);
        org.aventyrs.core.character.services.DeterminationPointsService pd =
                new org.aventyrs.core.character.services.DeterminationPointsServiceImpl();
        int pdBefore = pd.getCurrentDeterminationPoints(character, sheet);
        assertFalse(sheet.canBreatheUnderwater());

        activeAbilityService.activate(character, sheet, PeritoActiveAbility.GUELRAS, 1);

        assertTrue(sheet.canBreatheUnderwater());
        assertEquals(pdBefore - 1, pd.getCurrentDeterminationPoints(character, sheet));
        sheet.finishTurn();
        assertTrue(sheet.canBreatheUnderwater(), "it lasts through everyone else's Turns");
        sheet.startTurn(2);
        assertFalse(sheet.canBreatheUnderwater(), "the next Rodada's 1PD falls due as the holder's Turn opens");
    }

    @Test
    void reiDaMontanhaBuysARodadaOfClingingToWallsAndCeilings() throws IllegalOperationException {
        Character character = perito()
                .skill(SkillType.ATLETISMO, trained(SkillType.ATLETISMO, 5,
                        List.of(AtletismoSpecialization.LEVANTAMENTO_DE_PESO)))
                .skillCompetencyAbility(AtletismoCompetencyAbility.ALPINISTA_VELOZ)
                .build();
        CharacterSheet sheet = acquire(character, PeritoFeat.REI_DA_MONTANHA);
        assertFalse(sheet.canClingToSurfaces());

        activeAbilityService.activate(character, sheet, PeritoFeat.REI_DA_MONTANHA.resolveActiveAbility().orElseThrow(),
                1);

        assertTrue(sheet.canClingToSurfaces());
        assertFalse(sheet.canBreatheUnderwater(), "the two states are distinct");
    }
}
