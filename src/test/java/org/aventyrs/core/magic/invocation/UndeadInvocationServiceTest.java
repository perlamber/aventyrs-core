package org.aventyrs.core.magic.invocation;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.character.SizeCategory;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.HitPointsServiceImpl;
import org.aventyrs.core.effect.PossessaoFuriosa;
import org.aventyrs.core.monster.MonsterSheet;
import org.aventyrs.core.monster.summon.Abantesma;
import org.aventyrs.core.monster.summon.Carnical;
import org.aventyrs.core.monster.summon.UndeadSummonKind;
import org.aventyrs.core.scene.Scene;
import org.aventyrs.core.scene.SceneSummon;
import org.aventyrs.core.scene.SummonSpawner;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.SkillGraduation;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.subordinate.Subordinate;
import org.aventyrs.core.subordinate.SubordinateBenefit;
import org.aventyrs.core.util.DiceRoller;
import org.aventyrs.core.util.TranslatableMessages;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The REANIMAR tree's invocations (core 0.1.6.25). */
class UndeadInvocationServiceTest {

    private final UndeadInvocationService service = new UndeadInvocationServiceImpl();
    private final Player gm = new Player();
    private CharacterSheet necromancer;
    private CharacterSheet ogre;
    private Scene scene;

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        necromancer = caster(3, 4);
        ogre = CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK)
                .sizeCategory(SizeCategory.PLUS_THREE).build(), new Player());
        kill(ogre);
        scene = new Scene();
        scene.addParticipant(necromancer, 20, UUID.randomUUID());
        scene.addParticipant(ogre, 10, UUID.randomUUID());
        scene.startCombat();
        assertSame(necromancer, scene.next());
    }

    private static CharacterSheet caster(final int mana, final int focus) {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributes(CharacterAttributes.builder()
                        .focus(AttributeValue.builder().domain(AttributeDomain.FOCUS).base(focus).build())
                        .build())
                .skill(SkillType.DOMINIO_DO_MANA, CharacterSkill.builder()
                        .skill(SkillType.DOMINIO_DO_MANA.newSkillInstance()).specializations(List.of())
                        .graduation(SkillGraduation.builder().graduationValue(mana).build()).build())
                .build();
        return CharacterSheet.of(character, new Player());
    }

    private static void kill(final CombatantSheet sheet) {
        sheet.applyDamage(new HitPointsServiceImpl().getMaxHitPoints(sheet.getCharacter(), sheet) * 2);
    }

    private CharacterSheet corpse(final SizeCategory size) {
        CharacterSheet body = CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK)
                .sizeCategory(size).build(), new Player());
        kill(body);
        scene.addParticipant(body, 1, UUID.randomUUID());
        return body;
    }

    // ---------- Reanimar ----------

    @Test
    void reanimarRaisesAZumbiTheSizeOfItsCorpseHeldByConcentracao() {
        UndeadPlan plan = service.planReanimar(necromancer, ogre);
        SceneSummon zumbi = service.raise(scene, necromancer, ogre, plan, gm);

        assertEquals(UndeadSummonKind.ZUMBI, plan.kind());
        assertEquals(SizeCategory.PLUS_TWO, zumbi.getSummon().getCharacter().getSizeCategory(), "clamped to +2");
        assertTrue(zumbi.isSustained());
        assertTrue(zumbi.isRaisedFromCorpse());
        assertTrue(zumbi.getSummon().hasUndeadAnatomy());
        assertTrue(necromancer.isConcentrating());
    }

    @Test
    void theLivingCannotBeRaised() {
        UndeadPlan plan = service.planReanimar(necromancer, necromancer);
        IllegalOperationException refused = assertThrows(IllegalOperationException.class,
                () -> service.raise(scene, necromancer, necromancer, plan, gm));
        assertEquals(TranslatableMessages.RAISE_REQUIRES_CORPSE, refused.getMessage());
    }

    @Test
    void servoCadavericoMakesTheCorpseATorreProdigiosoForTwoRodadas() {
        int round = scene.getCurrentRound();
        Subordinate servant = service.servoCadaverico(scene, necromancer, ogre, SubordinateBenefit.TORRE_DEFESAS,
                true, null);

        assertEquals(ogre.getId(), servant.getCreatureId());
        assertTrue(scene.getAllParticipants().contains(ogre), "the corpse stays as the servant");
        assertEquals(1, ogre.getTimesRaised());
        assertTrue(servant.isProdigiousAt(round));
        assertTrue(servant.isProdigiousAt(round + 1));
        assertFalse(servant.isProdigiousAt(round + 2));
        assertTrue(necromancer.isConcentrating());
    }

    @Test
    void servoCadavericoIsOnlyACavaleiroOrATorre() {
        IllegalOperationException refused = assertThrows(IllegalOperationException.class,
                () -> service.servoCadaverico(scene, necromancer, ogre, SubordinateBenefit.BISPO_REGENERATION, false,
                        null));
        assertEquals(TranslatableMessages.SERVO_CADAVERICO_GRADE_NOT_ALLOWED, refused.getMessage());
    }

    // ---------- Criar Carniçal ----------

    @Test
    void aCarnicalCarriesItsStatBlockAndTiers() {
        MonsterSheet plain = Carnical.summonedBy(3).spawn(gm);
        MonsterSheet seventh = Carnical.summonedBy(7).spawn(gm);

        assertEquals(26, new HitPointsServiceImpl().getMaxHitPoints(plain.getCharacter(), plain), "26PV, x4");
        assertEquals(Carnical.DEFENSES, plain.getPhysicalDefense());
        assertEquals(Carnical.ACTION_POINTS, plain.getCharacter().getActionPoints());
        assertEquals(8, seventh.getCharacter().getAttributes().getStrength().getTotal(), "Força 6 + 2 at 7");
        assertTrue(plain.hasUndeadAnatomy());
    }

    @Test
    void acougueiroHasNoDuracaoCostsFiveMoreAndHitsHarder() {
        UndeadPlan plan = service.planCarnical(necromancer, ogre, true);
        SceneSummon butcher = service.raise(scene, necromancer, ogre, plan, gm);

        assertNull(butcher.getRemainingRounds());
        assertFalse(butcher.isSustained());
        assertEquals(UndeadInvocationService.BUTCHER_EXTRA_MANA, plan.extraManaCost());
        assertEquals(Carnical.ACTION_POINTS + 1, butcher.getSummon().getCharacter().getActionPoints());
        assertEquals(Carnical.BUTCHER_LIFE_STEAL, butcher.getSummon().getTotalLifeSteal());
    }

    @Test
    void anAcougueiroCarnicalAndAnyOtherCarnicalExcludeEachOther() {
        service.raise(scene, necromancer, ogre, service.planCarnical(necromancer, ogre, true), gm);
        CharacterSheet second = corpse(SizeCategory.ZERO);

        IllegalOperationException refused = assertThrows(IllegalOperationException.class,
                () -> service.raise(scene, necromancer, second, service.planCarnical(necromancer, second, false), gm));
        assertEquals(TranslatableMessages.BUTCHER_CARNICAL_EXCLUSIVE, refused.getMessage());
    }

    @Test
    void theFocoLimitStopsAFourthInvocation() {
        CharacterSheet novice = caster(3, 1);
        scene.addParticipant(novice, 15, UUID.randomUUID());
        service.raise(scene, novice, ogre, service.planReanimar(novice, ogre), gm);
        CharacterSheet second = corpse(SizeCategory.ZERO);

        IllegalOperationException refused = assertThrows(IllegalOperationException.class,
                () -> service.raise(scene, novice, second, service.planReanimar(novice, second), gm));
        assertEquals(TranslatableMessages.SUMMON_LIMIT_REACHED, refused.getMessage());
    }

    // ---------- Festim dos Mortos ----------

    @Test
    void theSarcophagusTableReadsTheDie() {
        assertEquals(UndeadSummonKind.ZUMBI, FestimDosMortos.kindFor(1));
        assertEquals(UndeadSummonKind.ZUMBI, FestimDosMortos.kindFor(3));
        assertEquals(UndeadSummonKind.CARNICAL, FestimDosMortos.kindFor(4));
        assertEquals(UndeadSummonKind.CARNICAL, FestimDosMortos.kindFor(5));
        assertEquals(UndeadSummonKind.ABANTESMA, FestimDosMortos.kindFor(6));
        assertEquals(2, FestimDosMortos.alliedUndeadDifficultyReduction(10, true));
        assertEquals(0, FestimDosMortos.alliedUndeadDifficultyReduction(9, true));
    }

    @Test
    void festimRaisesOneARodadaPastTheLimitUncontrolledAndAllLeaveWithIt() {
        CharacterSheet novice = caster(3, 1);
        scene.addParticipant(novice, 15, UUID.randomUUID());

        SummonSpawner sarcophagus = service.openSarcophagus(scene, novice, false, DiceRoller.fixed(2, 4, 6), gm);
        assertEquals(1, scene.getSummonsOf(novice).size());
        assertFalse(scene.getSummonsOf(novice).get(0).isUncontrolled(), "within Foco 1");

        untilNextRodada();
        List<SceneSummon> risen = scene.getSummonsOf(novice);
        assertEquals(2, risen.size());
        assertTrue(risen.get(1).isUncontrolled(), "the second goes past Foco 1");

        while (scene.getSummonSpawners().contains(sarcophagus)) {
            scene.next();
        }
        assertTrue(scene.getSummonsOf(novice).isEmpty(), "they go with the sarcophagus");
        assertTrue(scene.getAllParticipants().stream().noneMatch(CombatantSheet::isCorpse), "and leave no body");
    }

    @Test
    void hordaFamintaRisesTwoEachRodadaAndControlsNone() {
        service.openSarcophagus(scene, necromancer, true, DiceRoller.fixed(5), gm);

        List<SceneSummon> risen = scene.getSummonsOf(necromancer);
        assertEquals(2, risen.size());
        assertTrue(risen.stream().allMatch(SceneSummon::isUncontrolled));
        assertEquals(0, scene.countControlledSummons(necromancer));
    }

    @Test
    void festimsCreaturesWaitForTheNextRodada() {
        service.openSarcophagus(scene, necromancer, false, DiceRoller.fixed(1), gm);
        CombatantSheet risen = scene.getSummonsOf(necromancer).get(0).getSummon();
        assertFalse(scene.getParticipantsInInitiativeOrder().contains(risen), "risen, but not yet in the order");
        assertTrue(scene.getPendingParticipants().contains(risen));
    }

    // ---------- Abantesma ----------

    @Test
    void anAbantesmaLosesARodadaInsteadOfPv() {
        MonsterSheet ghost = Abantesma.summonedBy(0).spawn(gm);
        SceneSummon held = scene.addSummon(necromancer, ghost, null, Abantesma.ROUNDS);

        ghost.applyDamage(3);
        scene.settleSummons();

        assertEquals(Abantesma.ROUNDS - 1, held.getRemainingRounds());
        assertEquals(0, ghost.getDamageTaken());
        assertTrue(ghost.isImmuneToDamage(DamageType.FISICO, null), "incorpóreo");
        assertFalse(ghost.isImmuneToDamage(DamageType.ELEMENTAL, null));
        assertInstanceOf(PossessaoFuriosa.class, ghost.getAttackEffectChains().get(0));
    }

    private void untilNextRodada() {
        int round = scene.getCurrentRound();
        while (scene.getCurrentRound() == round) {
            scene.next();
        }
    }
}
