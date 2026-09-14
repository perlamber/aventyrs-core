package org.aventyrs.core.character.services;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.fixture.CharacterSkillFixture;
import org.aventyrs.core.feat.MobilidadeFeat;
import org.aventyrs.core.monster.AbstractMonsterTemplate;
import org.aventyrs.core.monster.GenericMonster;
import org.aventyrs.core.monster.MonsterSheet;
import org.aventyrs.core.scene.Scene;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.sheet.Hidden;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.DifficultyLevel;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.skill.furtividade.FurtividadeInteraction;
import org.aventyrs.core.skill.furtividade.FurtividadeSpecialization;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Esconder-se, end to end: a Furtividade total becomes a Grau de Dificuldade, each observer
 * opposes it once with their own Atenção, and the whole concealment ends the moment its holder
 * does something that gives them away.
 *
 * <p>Every case drives the real {@code CombatantSheet} state rather than a stubbed one — the
 * point of the mechanic is that the GD survives the roll that produced it, so the tests read it
 * back off the sheet the way a consumer would.
 *
 * <p>The concealment is a {@code DifficultyLevel} plus a flat bonus, so the two Especializações of
 * this contest — Maestria da Ocultação hiding, Sentidos Apurados watching — are what most of the
 * threshold cases here are about.
 */
class HidingServiceImplTest {

    /** A plain Furtividade total: Médio (18) +1, so an ordinary watcher needs exactly 19 back. */
    private static final int CONCEALMENT = 19;

    private final HidingService hidingService = new HidingServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        CharacterSkillFixture.loadTemplates();
    }

    private static CharacterSheet combatant() {
        return CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK)
                .id(UUID.randomUUID())
                .feats(new ArrayList<>())
                .build(), new Player());
    }

    /** A character who moves without giving themselves away — {@code MOVIMENTO_FURTIVO}. */
    private static CharacterSheet stealthyMover() {
        CharacterSheet sheet = combatant();
        sheet.getCharacter().grantFeat(MobilidadeFeat.MOVIMENTO_FURTIVO);
        return sheet;
    }

    private static MonsterSheet foeSpottingAt(final int perception) {
        return AbstractMonsterTemplate.builder()
                .name("Sentinela")
                .physicalDefense(13).magicDefense(11)
                .perception(perception)
                .build()
                .spawn(new Player());
    }

    /** The hider's own snapshot: everyone listed is in their sub-group. */
    private static SceneContext alongside(final CombatantSheet... allies) {
        return new SceneContext(List.of(allies), List.of(), Map.of());
    }

    private static SceneContext against(final CombatantSheet... enemies) {
        return new SceneContext(List.of(), List.of(enemies), Map.of());
    }

    /** Destreza 3 + 5 Graduações em Furtividade, holding Maestria da Ocultação — a roll bonus of 8. */
    private static CharacterSheet trainedHider() {
        CharacterSkill furtividade = CharacterSkillFixture.blank(CharacterSkillFixture.FURTIVIDADE_1)
                .specializations(List.of(FurtividadeSpecialization.MAESTRIA_DA_OCULTACAO))
                .build();
        furtividade.increaseGraduation(5);
        return CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK)
                .id(UUID.randomUUID())
                .feats(new ArrayList<>())
                .attributes(CharacterAttributes.builder()
                        .dexterity(AttributeValue.builder().domain(AttributeDomain.DEXTERITY).base(3).build())
                        .build())
                .skill(SkillType.FURTIVIDADE, furtividade)
                .build(), new Player());
    }

    // ---------- the documented flow, through the real Interaction ----------

    /**
     * The whole cycle a consumer runs, driven through {@code FurtividadeInteraction} rather than
     * by handing the service a number: roll, hide on the total, let two watchers look, give
     * yourself away.
     *
     * <p>It also pins the claim {@code hide}'s javadoc makes — that the tier stored is the one the
     * roll itself already reported. The Interaction thresholds a held Especialização against
     * {@code getExpertValue()}, so its {@code reachedDifficultyLevel} and {@code
     * Hidden#getDifficultyLevel()} must agree, or the flag passed to {@code hide} means something
     * different from what the roll meant.
     */
    @Test
    void theDocumentedFlowFromFurtividadeRollToRevealHangsTogether() {
        CharacterSheet hider = trainedHider();
        CharacterSheet ordinary = combatant();
        CharacterSheet sharp = combatant();
        SceneContext hiderContext = against(ordinary, sharp);

        SkillRoll roll = new SkillRoll(List.of(6, 4, 3), FurtividadeSpecialization.MAESTRIA_DA_OCULTACAO);
        InteractionResult rolled = new FurtividadeInteraction().applyTo(hider, hiderContext, roll);
        int total = rolled.getSkillRollBonus() + roll.getTotal();
        assertEquals(21, total);

        Hidden hidden = hidingService.hide(hider, total, true);

        assertEquals(DifficultyLevel.HARD, rolled.getReachedDifficultyLevel());
        assertEquals(rolled.getReachedDifficultyLevel(), hidden.getDifficultyLevel());
        assertEquals(1, hidden.getBonus());

        // The same 21 finds the ordinary watcher short and the specialised one exactly on target.
        assertFalse(hidingService.resolveDetection(hider, ordinary, 21, false, hiderContext));
        assertTrue(hidingService.resolveDetection(hider, sharp, 21, true, hiderContext));

        assertTrue(hidingService.isHiddenFrom(hider, ordinary, hiderContext));
        assertFalse(hidingService.isHiddenFrom(hider, sharp, hiderContext));

        assertTrue(hidingService.reveal(hider, RevealTrigger.ATTACK));
        assertFalse(hidingService.isHiddenFrom(hider, ordinary, hiderContext));
    }

    // ---------- the value the roll leaves behind ----------

    @Test
    void hidingAppliesEscondidoCarryingTheGrauDeDificuldade() {
        CharacterSheet hider = combatant();

        Hidden hidden = hidingService.hide(hider, CONCEALMENT);

        assertTrue(hider.hasCondition(ConditionType.ESCONDIDO, null));
        assertEquals(DifficultyLevel.MEDIUM, hidden.getDifficultyLevel());
        assertEquals(1, hidden.getBonus());
        assertEquals(CONCEALMENT, hidden.getConcealmentValue());
        assertEquals(CONCEALMENT, hidingService.getConcealmentValue(hider));
        assertEquals(hidden, hider.getHidden().orElseThrow());
    }

    /**
     * The worked example from {@code Hidden}: 21 rolled with Maestria da Ocultação is Difícil +1,
     * because Difícil's expert threshold is 20. The same 21 rolled raw only reaches Médio +3.
     */
    @Test
    void theSpecializationDecomposesTheSameTotalIntoAHigherTier() {
        CharacterSheet expert = combatant();
        CharacterSheet amateur = combatant();

        Hidden expertly = hidingService.hide(expert, 21, true);
        Hidden plainly = hidingService.hide(amateur, 21, false);

        assertEquals(DifficultyLevel.HARD, expertly.getDifficultyLevel());
        assertEquals(1, expertly.getBonus());
        assertEquals(DifficultyLevel.MEDIUM, plainly.getDifficultyLevel());
        assertEquals(3, plainly.getBonus());
    }

    /**
     * And that tier is what each side's Especialização is worth. An ordinary watcher faces the
     * expert hider's tier at its base value (23+1); a watcher with Sentidos Apurados faces it at
     * the expert value (20+1) — the raw 21, as if neither had specialised.
     */
    @Test
    void sentidosApuradosTakesBackWhatMaestriaDaOcultacaoGained() {
        CharacterSheet hider = combatant();
        hidingService.hide(hider, 21, true);

        assertEquals(24, hidingService.getConcealmentValue(hider));
        assertEquals(21, hidingService.getConcealmentValue(hider, true));
    }

    @Test
    void anObserverWithSentidosApuradosSeesThroughWhatAnOrdinaryOneMisses() {
        CharacterSheet hider = combatant();
        CharacterSheet sharp = combatant();
        CharacterSheet ordinary = combatant();
        hidingService.hide(hider, 21, true);

        assertFalse(hidingService.resolveDetection(hider, ordinary, 21, false, against(sharp, ordinary)));
        assertTrue(hidingService.resolveDetection(hider, sharp, 21, true, against(sharp, ordinary)));
    }

    /** A GD stated outright, for a caller that has the tier rather than a roll to decompose. */
    @Test
    void aConcealmentCanBeStatedAsATierAndABonus() {
        CharacterSheet hider = combatant();

        Hidden hidden = hidingService.hide(hider, DifficultyLevel.VERY_HARD, 2);

        assertEquals(DifficultyLevel.VERY_HARD, hidden.getDifficultyLevel());
        assertEquals(DifficultyLevel.VERY_HARD.getBaseValue() + 2, hidingService.getConcealmentValue(hider));
        assertEquals(DifficultyLevel.VERY_HARD.getExpertValue() + 2, hidingService.getConcealmentValue(hider, true));
    }

    /**
     * A total too low to reach even Muito Fácil clamps to that tier with a negative bonus, so the
     * GD still reproduces the roll. Hiding badly is a bad hiding place, not a failure to hide.
     */
    @Test
    void aTotalBelowEveryTierStillConcealsAtWhatItRolled() {
        CharacterSheet hider = combatant();

        Hidden hidden = hidingService.hide(hider, 9);

        assertEquals(DifficultyLevel.VERY_EASY, hidden.getDifficultyLevel());
        assertEquals(9, hidden.getConcealmentValue());
    }

    /** The condition is open-ended: hiding does not lapse on a Rodada count, it is revealed. */
    @Test
    void hidingSurvivesTheRodadaBoundary() {
        CharacterSheet hider = combatant();
        hidingService.hide(hider, CONCEALMENT);

        hider.finishTurn();
        hider.startNewRound();

        assertEquals(CONCEALMENT, hidingService.getConcealmentValue(hider));
    }

    @Test
    void aCharacterWhoNeverHidHasNoConcealmentValue() {
        assertNull(hidingService.getConcealmentValue(combatant()));
        assertFalse(hidingService.isHiddenFrom(combatant(), combatant(), null));
    }

    // ---------- who the concealment works on ----------

    @Test
    void anEnemyWhoHasNotLookedIsFooled() {
        CharacterSheet hider = combatant();
        CharacterSheet foe = combatant();
        hidingService.hide(hider, CONCEALMENT);

        assertTrue(hidingService.isHiddenFrom(hider, foe, against(foe)));
    }

    /** "Personagens fora do seu grupo" — your own group sees you standing there. */
    @Test
    void nobodyHidesFromTheirOwnGroup() {
        CharacterSheet hider = combatant();
        CharacterSheet ally = combatant();
        hidingService.hide(hider, CONCEALMENT);

        assertFalse(hidingService.isHiddenFrom(hider, ally, alongside(ally)));
        assertTrue(hidingService.resolveDetection(hider, ally, null, alongside(ally)));
    }

    @Test
    void aCharacterIsNeverHiddenFromThemselves() {
        CharacterSheet hider = combatant();
        hidingService.hide(hider, CONCEALMENT);

        assertFalse(hidingService.isHiddenFrom(hider, hider, against()));
    }

    /**
     * The group test is the one place a {@code null} snapshot reads as "outside the group" rather
     * than as the usual "condition not met" — a caller with no Scene keeps the concealment it was
     * just granted, and being hidden from an ally costs that ally nothing this core models.
     */
    @Test
    void noSceneContextLeavesTheConcealmentStanding() {
        CharacterSheet hider = combatant();
        hidingService.hide(hider, CONCEALMENT);

        assertTrue(hidingService.isHiddenFrom(hider, combatant(), null));
    }

    // ---------- opposing it with Atenção ----------

    @Test
    void anAttentionRollBelowTheConcealmentSeesNothing() {
        CharacterSheet hider = combatant();
        CharacterSheet foe = combatant();
        hidingService.hide(hider, CONCEALMENT);

        assertFalse(hidingService.resolveDetection(hider, foe, CONCEALMENT - 1, against(foe)));
        assertTrue(hidingService.isHiddenFrom(hider, foe, against(foe)));
    }

    /** Reaching the GD exactly succeeds — the same reading of a tie every other roll takes. */
    @Test
    void meetingTheConcealmentExactlySeesThrough() {
        CharacterSheet hider = combatant();
        CharacterSheet foe = combatant();
        hidingService.hide(hider, CONCEALMENT);

        assertTrue(hidingService.resolveDetection(hider, foe, CONCEALMENT, against(foe)));
        assertFalse(hidingService.isHiddenFrom(hider, foe, against(foe)));
    }

    /** Detection is per observer: one sentry's success does not open the other's eyes. */
    @Test
    void spottingIsRecordedPerObserver() {
        CharacterSheet hider = combatant();
        CharacterSheet sharp = combatant();
        CharacterSheet dull = combatant();
        hidingService.hide(hider, CONCEALMENT);

        assertTrue(hidingService.resolveDetection(hider, sharp, CONCEALMENT + 4, against(sharp, dull)));
        assertFalse(hidingService.resolveDetection(hider, dull, CONCEALMENT - 4, against(sharp, dull)));

        assertFalse(hidingService.isHiddenFrom(hider, sharp, against(sharp, dull)));
        assertTrue(hidingService.isHiddenFrom(hider, dull, against(sharp, dull)));
    }

    /** Once seen, seen — the observer does not have to roll again every Rodada. */
    @Test
    void anObserverWhoSawThroughItKeepsSeeing() {
        CharacterSheet hider = combatant();
        CharacterSheet foe = combatant();
        hidingService.hide(hider, CONCEALMENT);
        hidingService.resolveDetection(hider, foe, CONCEALMENT, against(foe));

        assertTrue(hidingService.resolveDetection(hider, foe, null, against(foe)));
    }

    /** A rolling observer with no total is "cannot tell", and sees nothing rather than everything. */
    @Test
    void noAttentionTotalSeesNothing() {
        CharacterSheet hider = combatant();
        CharacterSheet foe = combatant();
        hidingService.hide(hider, CONCEALMENT);

        assertFalse(hidingService.resolveDetection(hider, foe, null, against(foe)));
        assertTrue(hidingService.isHiddenFrom(hider, foe, against(foe)));
    }

    @Test
    void resolvingAgainstSomeoneNotHiddenSeesThem() {
        assertTrue(hidingService.resolveDetection(combatant(), combatant(), null, against()));
    }

    // ---------- a foe never rolls ----------

    @Test
    void aFoeOpposesWithItsAuthoredPerception() {
        CharacterSheet hider = combatant();
        MonsterSheet sharpEyed = foeSpottingAt(CONCEALMENT + 1);
        MonsterSheet oblivious = foeSpottingAt(CONCEALMENT - 1);
        hidingService.hide(hider, CONCEALMENT);

        assertTrue(hidingService.resolveDetection(hider, sharpEyed, null, against(sharpEyed, oblivious)));
        assertFalse(hidingService.resolveDetection(hider, oblivious, null, against(sharpEyed, oblivious)));
    }

    /**
     * A total handed in for a foe is ignored, and so is an Especialização claimed on its behalf:
     * the stat block is the whole of its answer, so it faces the tier's ordinary threshold.
     */
    @Test
    void aRolledTotalOrSpecializationSuppliedForAFoeIsIgnored() {
        CharacterSheet hider = combatant();
        MonsterSheet oblivious = foeSpottingAt(CONCEALMENT - 1);
        hidingService.hide(hider, CONCEALMENT);

        assertFalse(hidingService.resolveDetection(hider, oblivious, CONCEALMENT + 10, true, against(oblivious)));
    }

    @Test
    void aFoeWithNoAuthoredPerceptionUsesTheDefault() {
        assertEquals(HidingService.DEFAULT_MONSTER_PERCEPTION,
                AbstractMonsterTemplate.builder().name("Anônimo").build().getPerception());

        CharacterSheet hider = combatant();
        MonsterSheet foe = AbstractMonsterTemplate.builder().name("Anônimo").build().spawn(new Player());
        hidingService.hide(hider, HidingService.DEFAULT_MONSTER_PERCEPTION);

        assertTrue(hidingService.resolveDetection(hider, foe, null, against(foe)));
    }

    /** The archetypes rise together, and their eyes rise with them. */
    @Test
    void theGenericArchetypesGetSharperByTier() {
        assertTrue(GenericMonster.CAPANGA.getPerception() < GenericMonster.ABERRACAO.getPerception());
        assertEquals(GenericMonster.CAPANGA.getPerception(),
                GenericMonster.CAPANGA.spawn(new Player()).getPerception());
    }

    // ---------- giving yourself away ----------

    @Test
    void attackingRevealsOutright() {
        CharacterSheet hider = combatant();
        CharacterSheet foe = combatant();
        hidingService.hide(hider, CONCEALMENT);

        assertTrue(hidingService.reveals(hider, RevealTrigger.ATTACK));
        assertTrue(hidingService.reveal(hider, RevealTrigger.ATTACK));

        assertFalse(hider.hasCondition(ConditionType.ESCONDIDO, null));
        assertFalse(hidingService.isHiddenFrom(hider, foe, against(foe)));
    }

    @Test
    void rollingAgainstSomebodyElseReveals() {
        CharacterSheet hider = combatant();
        hidingService.hide(hider, CONCEALMENT);

        assertTrue(hidingService.reveal(hider, RevealTrigger.SKILL_ROLL_ON_ANOTHER));
        assertNull(hidingService.getConcealmentValue(hider));
    }

    @Test
    void movingRevealsByDefault() {
        CharacterSheet hider = combatant();
        hidingService.hide(hider, CONCEALMENT);

        assertTrue(hidingService.reveal(hider, RevealTrigger.MOVEMENT));
        assertNull(hidingService.getConcealmentValue(hider));
    }

    /**
     * {@code MobilidadeFeat#MOVIMENTO_FURTIVO} — "você pode se mover enquanto furtivo". It excuses
     * movement and nothing else: the same holder attacking is given away like anyone else, which is
     * what stops one clause quietly becoming permanent invisibility.
     */
    @Test
    void movimentoFurtivoExcusesMovementAndNothingElse() {
        CharacterSheet hider = stealthyMover();
        hidingService.hide(hider, CONCEALMENT);

        assertFalse(hidingService.reveals(hider, RevealTrigger.MOVEMENT));
        assertFalse(hidingService.reveal(hider, RevealTrigger.MOVEMENT));
        assertEquals(CONCEALMENT, hidingService.getConcealmentValue(hider));

        assertTrue(hidingService.reveal(hider, RevealTrigger.ATTACK));
        assertNull(hidingService.getConcealmentValue(hider));
    }

    @Test
    void revealingSomebodyWhoIsNotHiddenDoesNothing() {
        CharacterSheet standing = combatant();

        assertFalse(hidingService.reveals(standing, RevealTrigger.ATTACK));
        assertFalse(hidingService.reveal(standing, RevealTrigger.ATTACK));
    }

    /** A new Furtividade roll is a new place to hide — whoever had spotted the old one starts over. */
    @Test
    void hidingAgainForgetsWhoHadSpottedYou() {
        CharacterSheet hider = combatant();
        CharacterSheet foe = combatant();
        hidingService.hide(hider, CONCEALMENT);
        hidingService.resolveDetection(hider, foe, CONCEALMENT, against(foe));
        hidingService.reveal(hider, RevealTrigger.ATTACK);

        hidingService.hide(hider, CONCEALMENT + 2);

        assertTrue(hidingService.isHiddenFrom(hider, foe, against(foe)));
        assertEquals(CONCEALMENT + 2, hidingService.getConcealmentValue(hider));
    }

    /** The group test reads a real {@code Scene}'s sub-groups, not just a hand-built snapshot. */
    @Test
    void groupMembershipComesFromTheScene() {
        CharacterSheet hider = combatant();
        CharacterSheet ally = combatant();
        CharacterSheet foe = combatant();
        Scene scene = new Scene();
        UUID party = UUID.randomUUID();
        scene.addParticipant(hider, 12, party);
        scene.addParticipant(ally, 10, party);
        scene.addParticipant(foe, 8, UUID.randomUUID());
        hidingService.hide(hider, CONCEALMENT);

        SceneContext context = scene.buildContext(hider, Map.of());

        assertFalse(hidingService.isHiddenFrom(hider, ally, context));
        assertTrue(hidingService.isHiddenFrom(hider, foe, context));
    }
}
