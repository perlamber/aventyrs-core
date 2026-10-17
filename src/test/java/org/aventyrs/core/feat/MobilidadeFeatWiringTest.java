package org.aventyrs.core.feat;

import org.aventyrs.core.action.ActionPointsServiceImpl;
import org.aventyrs.core.action.Manoeuvre;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.services.ChargeServiceImpl;
import org.aventyrs.core.item.HeavyBladeItem;
import org.aventyrs.core.sheet.LifeSteal;
import org.aventyrs.core.sheet.TemporaryEffect;
import org.aventyrs.core.character.TitleSlot;
import org.aventyrs.core.title.senhordabriga.SenhorDaBriga;
import org.aventyrs.core.character.CharacterEgos;
import org.aventyrs.core.character.EgoValue;
import org.aventyrs.core.character.services.InitiativeBlessingServiceImpl;
import org.aventyrs.core.ego.InitiativeAdvantage;
import org.aventyrs.core.scene.MovementHeading;
import org.aventyrs.core.scene.Scene;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.services.HidingServiceImpl;
import org.aventyrs.core.character.services.RepositionServiceImpl;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.character.services.MovementServiceImpl;
import org.aventyrs.core.sheet.ActionOutcome;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.combat.AttackReceiver;
import org.aventyrs.core.combat.IncomingAttack;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.skill.DifficultyLevel;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.fixture.CharacterSkillFixture;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantAction;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The MobilidadeFeat wiring (core 0.1.7.17+, docs/plans/mobilidade-wiring-plan.md). */
class MobilidadeFeatWiringTest {

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        CharacterSkillFixture.loadTemplates();
    }

    static CharacterSheet sheetWith(final Feat... feats) {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .feats(new ArrayList<>(List.of(feats)))
                .build();
        return CharacterSheet.of(character, new Player());
    }

    static CombatantAction attackAction() {
        return new CombatantAction(SkillType.ATAQUE_CORPO_A_CORPO, null, null, ActionCost.ofActionPoints(2), 0, null);
    }

    @Nested
    class MovementLedger {

        @Test
        void distanceAccumulatesWithinATurnAndResetsAtTheNext() {
            CharacterSheet sheet = sheetWith();
            sheet.startTurn(0);
            sheet.recordDistanceMoved(2);
            sheet.recordDistanceMoved(1);
            sheet.recordDistanceMoved(-4);
            assertEquals(3, sheet.getDistanceMovedThisTurn());
            assertTrue(sheet.hasMovedThisTurn());

            sheet.finishTurn();
            sheet.startTurn(1);
            assertEquals(0, sheet.getDistanceMovedThisTurn());
            assertFalse(sheet.hasMovedThisTurn());
        }

        @Test
        void aRepositionOrABoughtMovementCountsAsMoving() {
            CharacterSheet repositioned = sheetWith();
            repositioned.startTurn(0);
            repositioned.consumeRepositionThisRound();
            assertTrue(repositioned.hasMovedThisTurn());

            CharacterSheet walked = sheetWith();
            walked.startTurn(0);
            walked.consumeMovementThisRound();
            assertTrue(walked.hasMovedThisTurn());
        }

        @Test
        void theMovementCountIsStampedWhenAnActionIsRecorded() {
            CharacterSheet sheet = sheetWith();
            sheet.startTurn(0);
            assertEquals(-1, sheet.getMovementsAtLatestAction());
            sheet.consumeMovementThisRound();
            sheet.recordAction(attackAction());
            assertEquals(1, sheet.getMovementsAtLatestAction());

            sheet.finishTurn();
            sheet.startTurn(1);
            assertEquals(-1, sheet.getMovementsAtLatestAction());
        }
    }

    @Nested
    class Esquiva {

        private int defenceTotal(final CharacterSheet defender, final boolean reaction) {
            return new AttackReceiver().resolve(IncomingAttack.builder()
                    .defender(defender)
                    .difficultyLevel(DifficultyLevel.MEDIUM)
                    .defenseType(DefenseType.PHYSICAL)
                    .defenseRoll(new SkillRoll(List.of(3, 3, 3)))
                    .reaction(reaction)
                    .build()).getDefenseTotal();
        }

        @Test
        void addsTwoAgainstAReactionAttackOnly() {
            CharacterSheet holder = sheetWith(MobilidadeFeat.ESQUIVA);
            CharacterSheet plain = sheetWith();
            assertEquals(defenceTotal(plain, true) + 2, defenceTotal(holder, true));
            assertEquals(defenceTotal(plain, false), defenceTotal(holder, false));
            assertEquals(defenceTotal(plain, true), defenceTotal(plain, false));
        }

        @Test
        void movingGrantsOneDefesaUntilTheNextOwnTurn() {
            CharacterSheet holder = sheetWith(MobilidadeFeat.ESQUIVA);
            holder.startTurn(0);
            holder.consumeMovementThisRound();
            holder.finishTurn();
            assertEquals(1, holder.getTemporaryBonus(ModifierType.DEFESAS));

            holder.startTurn(1);
            assertEquals(0, holder.getTemporaryBonus(ModifierType.DEFESAS));
        }

        @Test
        void standingStillGrantsNothing() {
            CharacterSheet holder = sheetWith(MobilidadeFeat.ESQUIVA);
            holder.startTurn(0);
            holder.finishTurn();
            assertEquals(0, holder.getTemporaryBonus(ModifierType.DEFESAS));
        }
    }

    @Nested
    class SeMoverEAtacar {

        private int attackBonus(final CharacterSheet holder) {
            return MobilidadeFeat.SE_MOVER_E_ATACAR.resolveSkillRollBonus(SkillType.ATAQUE_CORPO_A_CORPO, null, null,
                    holder.getCharacter(), null, holder, null);
        }

        @Test
        void vantagemOnceThreeUdWereTravelledThisTurn() {
            CharacterSheet holder = sheetWith(MobilidadeFeat.ESQUIVA, MobilidadeFeat.SE_MOVER_E_ATACAR);
            holder.startTurn(0);
            holder.recordDistanceMoved(2);
            assertEquals(0, attackBonus(holder));
            holder.recordDistanceMoved(1);
            assertEquals(Skill.ADVANTAGE_BONUS, attackBonus(holder));
            assertEquals(0, MobilidadeFeat.SE_MOVER_E_ATACAR.resolveSkillRollBonus(SkillType.ATLETISMO, null, null,
                    holder.getCharacter(), null, holder, null));
        }

        @Test
        void twoMoreUdOnTheFirstMovementAfterAnAttackOnly() {
            CharacterSheet holder = sheetWith(MobilidadeFeat.ESQUIVA, MobilidadeFeat.SE_MOVER_E_ATACAR);
            CharacterSheet plain = sheetWith();
            MovementServiceImpl movement = new MovementServiceImpl();
            holder.startTurn(0);
            plain.startTurn(0);
            int base = movement.getMovementBase(plain, 0);
            assertEquals(base, movement.getMovementBase(holder, 0));

            holder.consumeMovementThisRound();
            holder.recordAction(attackAction());
            assertEquals(base + 2, movement.getMovementBase(holder, 1));
            assertEquals(base, movement.getMovementBase(holder, 2));
        }

        @Test
        void aPericiaCheckBeforeTheMovementGrantsNothing() {
            CharacterSheet holder = sheetWith(MobilidadeFeat.ESQUIVA, MobilidadeFeat.SE_MOVER_E_ATACAR);
            CharacterSheet plain = sheetWith();
            MovementServiceImpl movement = new MovementServiceImpl();
            holder.startTurn(0);
            holder.recordAction(new CombatantAction(SkillType.ATLETISMO, null, null, ActionCost.ofActionPoints(2), 0,
                    null));
            assertEquals(movement.getMovementBase(plain, 0), movement.getMovementBase(holder, 0));
        }
    }

    @Nested
    class InvestidaAquatica {

        private CombatantAction swim(final boolean succeeded) {
            return new CombatantAction(SkillType.ATLETISMO, null, null, ActionCost.ofActionPoints(2), 0,
                    new ActionOutcome(succeeded, 0, null, null), null, java.util.Set.of(), Manoeuvre.NADAR);
        }

        private int attackCost(final CharacterSheet attacker) {
            return new ActionPointsServiceImpl().getAttackCost(attacker, SkillType.ATAQUE_CORPO_A_CORPO, null,
                    java.util.Set.of(), 0).spentActionPoints();
        }

        @Test
        void anAttackRightAfterASuccessfulSwimCostsOnePaLess() {
            CharacterSheet holder = sheetWith(MobilidadeFeat.INVESTIDA_AQUATICA);
            holder.startTurn(0);
            int full = attackCost(holder);
            holder.recordAction(swim(true));
            assertEquals(full - 1, attackCost(holder));
        }

        @Test
        void aFailedSwimOrAnInterveningActionGivesNothing() {
            CharacterSheet failed = sheetWith(MobilidadeFeat.INVESTIDA_AQUATICA);
            failed.startTurn(0);
            int full = attackCost(failed);
            failed.recordAction(swim(false));
            assertEquals(full, attackCost(failed));

            CharacterSheet interrupted = sheetWith(MobilidadeFeat.INVESTIDA_AQUATICA);
            interrupted.startTurn(0);
            interrupted.recordAction(swim(true));
            interrupted.recordAction(attackAction());
            assertEquals(full, attackCost(interrupted));
        }
    }

    @Nested
    class MovimentoFurtivo {

        private CharacterSheet hiderWith(final int graduations) {
            CharacterSkill furtividade = CharacterSkillFixture.blank(CharacterSkillFixture.FURTIVIDADE_1).build();
            furtividade.increaseGraduation(graduations);
            return CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK)
                    .feats(new ArrayList<>(List.of(MobilidadeFeat.MOVIMENTO_FURTIVO)))
                    .skill(SkillType.FURTIVIDADE, furtividade)
                    .build(), new Player());
        }

        @Test
        void halvedOnlyWhileEscondido() {
            CharacterSheet hider = hiderWith(2);
            MovementServiceImpl movement = new MovementServiceImpl();
            int open = movement.getMovementBase(hider, 0);
            new HidingServiceImpl().hide(hider, 19);
            assertEquals(open / 2, movement.getMovementBase(hider, 0));
        }

        @Test
        void notHalvedFromSevenGraduacoes() {
            CharacterSheet hider = hiderWith(7);
            assertTrue(hider.getCharacter().getEffectiveGraduation(SkillType.FURTIVIDADE) >= 7);
            MovementServiceImpl movement = new MovementServiceImpl();
            int open = movement.getMovementBase(hider, 0);
            new HidingServiceImpl().hide(hider, 19);
            assertEquals(open, movement.getMovementBase(hider, 0));
        }
    }

    @Nested
    class MovimentoAcrobatico {

        private final RepositionServiceImpl reposition = new RepositionServiceImpl();

        private CharacterSheet acrobat(final int dexterity) {
            return CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK)
                    .feats(new ArrayList<>(List.of(MobilidadeFeat.MOVIMENTO_ACROBATICO)))
                    .attributes(CharacterAttributes.builder()
                            .dexterity(AttributeValue.builder().domain(AttributeDomain.DEXTERITY).base(dexterity).build())
                            .build())
                    .build(), new Player());
        }

        @Test
        void reachesHalfTheDestrezaTotal() {
            CharacterSheet sheet = acrobat(5);
            assertEquals(sheet.getAttributeTotal(AttributeDomain.DEXTERITY) / 2, reposition.getAcrobaticDistance(sheet));
        }

        @Test
        void takesTheReposicionarAndOnlyOncePerRodada() {
            CharacterSheet sheet = acrobat(4);
            sheet.startTurn(0);
            assertTrue(reposition.canMoveAcrobatically(sheet, 0, null));
            reposition.beginAcrobatic(sheet, 0, null);
            assertEquals(1, sheet.getRepositionsTakenThisRound());
            assertFalse(reposition.canMoveAcrobatically(sheet, 0, null));
            assertThrows(IllegalOperationException.class, () -> reposition.beginAcrobatic(sheet, 0, null));

            sheet.finishTurn();
            sheet.startTurn(1);
            assertTrue(reposition.canMoveAcrobatically(sheet, 1, null));
        }

        @Test
        void refusedAfterMovingOnPontosDeAcaoOrWithoutTheTalento() {
            CharacterSheet sheet = acrobat(4);
            sheet.startTurn(0);
            sheet.consumeMovementThisRound();
            assertFalse(reposition.canMoveAcrobatically(sheet, 0, null));

            CharacterSheet plain = sheetWith();
            plain.startTurn(0);
            assertFalse(reposition.canMoveAcrobatically(plain, 0, null));
        }
    }

    @Nested
    class IniciativaAprimorada {

        private Character withIniciativa(final int base) {
            return CharacterFixture.blank(CharacterFixture.BLANK)
                    .egos(CharacterEgos.builder().iniciativa(EgoValue.builder().base(base).build()).build())
                    .feats(new ArrayList<>(List.of(new VantagemDeIniciativaAprimoradaFeat(InitiativeAdvantage.IMPETO))))
                    .build();
        }

        @Test
        void theVantagemArrivesOnlyOnceIniciativaReachesFour() {
            assertFalse(withIniciativa(2).getAllEgoAdvantages().contains(InitiativeAdvantage.IMPETO));
            Character atFour = withIniciativa(3);
            assertEquals(4, atFour.getEffectiveEgoTotal(org.aventyrs.core.character.EgoDomain.INICIATIVA));
            assertTrue(atFour.getAllEgoAdvantages().contains(InitiativeAdvantage.IMPETO));
        }

        @Test
        void theBareConstantDeclaresTheChoice() {
            Character holder = CharacterFixture.blank(CharacterFixture.BLANK).feats(new ArrayList<>()).build();
            List<FeatChoice<?>> choices = MobilidadeFeat.INICIATIVA_APRIMORADA.resolveRequiredChoices(holder);
            assertEquals(1, choices.size());
            assertFalse(choices.get(0).options().isEmpty());
            assertTrue(new VantagemDeIniciativaAprimoradaFeat(InitiativeAdvantage.IMPETO).catalogEntry()
                    == MobilidadeFeat.INICIATIVA_APRIMORADA);
        }
    }

    @Nested
    class PortaEstandarte {

        private Scene sceneWith(final CharacterSheet bearer, final CharacterSheet ally, final CharacterSheet foe,
                                final int bearerInitiative) {
            Scene scene = new Scene();
            UUID heroes = UUID.randomUUID();
            scene.addParticipant(bearer, bearerInitiative, heroes);
            scene.addParticipant(ally, 10, heroes);
            scene.addParticipant(foe, 12, UUID.randomUUID());
            scene.startCombat();
            return scene;
        }

        @Test
        void firstSlotGrantsTwoThenOnePaToTheBearerAndAllies() {
            CharacterSheet bearer = sheetWith(MobilidadeFeat.PORTA_ESTANDARTE);
            CharacterSheet ally = sheetWith();
            CharacterSheet foe = sheetWith();
            Scene scene = sceneWith(bearer, ally, foe, 20);
            scene.applyFirstToActBlessings();
            assertEquals(2, bearer.getTemporaryBonus(ModifierType.ACTION_POINTS));
            assertEquals(2, ally.getTemporaryBonus(ModifierType.ACTION_POINTS));
            assertEquals(0, foe.getTemporaryBonus(ModifierType.ACTION_POINTS));

            scene.next();
            bearer.finishTurn();
            assertEquals(1, bearer.getTemporaryBonus(ModifierType.ACTION_POINTS));
        }

        @Test
        void notFirstGrantsNothing() {
            CharacterSheet bearer = sheetWith(MobilidadeFeat.PORTA_ESTANDARTE);
            CharacterSheet ally = sheetWith();
            CharacterSheet foe = sheetWith();
            Scene scene = sceneWith(bearer, ally, foe, 1);
            scene.applyFirstToActBlessings();
            assertEquals(0, bearer.getTemporaryBonus(ModifierType.ACTION_POINTS));
            assertEquals(0, scene.getHeadingMovementBonus(bearer, new MovementHeading(Set.of(foe.getId()))));
        }

        @Test
        void headingTowardAnEnemyOrTheBearerIsWorthTwoUd() {
            CharacterSheet bearer = sheetWith(MobilidadeFeat.PORTA_ESTANDARTE);
            CharacterSheet ally = sheetWith();
            CharacterSheet foe = sheetWith();
            Scene scene = sceneWith(bearer, ally, foe, 20);
            assertEquals(2, scene.getHeadingMovementBonus(bearer, new MovementHeading(Set.of(foe.getId()))));
            assertEquals(0, scene.getHeadingMovementBonus(bearer, new MovementHeading(Set.of(ally.getId()))));
            assertEquals(2, scene.getHeadingMovementBonus(ally, new MovementHeading(Set.of(bearer.getId()))));
            assertEquals(0, scene.getHeadingMovementBonus(ally, new MovementHeading(Set.of(foe.getId()))));
            assertEquals(0, scene.getHeadingMovementBonus(foe, new MovementHeading(Set.of(bearer.getId()))));
        }

        @Test
        void headingBonusEndsAfterTheSecondRodada() {
            CharacterSheet bearer = sheetWith(MobilidadeFeat.PORTA_ESTANDARTE);
            CharacterSheet ally = sheetWith();
            CharacterSheet foe = sheetWith();
            Scene scene = sceneWith(bearer, ally, foe, 20);
            MovementHeading towardFoe = new MovementHeading(Set.of(foe.getId()));
            for (int turn = 0; turn < 6; turn++) {
                scene.next();
            }
            assertEquals(1, scene.getCurrentRound());
            assertEquals(2, scene.getHeadingMovementBonus(bearer, towardFoe));
            for (int turn = 0; turn < 3; turn++) {
                scene.next();
            }
            assertEquals(2, scene.getCurrentRound());
            assertEquals(0, scene.getHeadingMovementBonus(bearer, towardFoe));
        }
    }

    @Nested
    class LiderarOAvanco {

        @Test
        void everyWinnerHoldingItKeepsItsOwnBlessing() {
            CharacterSheet one = sheetWith(MobilidadeFeat.INICIATIVA_APRIMORADA, MobilidadeFeat.LIDERAR_O_AVANCO);
            CharacterSheet two = sheetWith(MobilidadeFeat.INICIATIVA_APRIMORADA, MobilidadeFeat.LIDERAR_O_AVANCO);
            CharacterSheet foe = sheetWith(MobilidadeFeat.LIDERAR_O_AVANCO);
            Scene scene = new Scene();
            UUID heroes = UUID.randomUUID();
            scene.addParticipant(one, 15, heroes);
            scene.addParticipant(two, 9, heroes);
            scene.addParticipant(foe, 12, UUID.randomUUID());
            InitiativeBlessingServiceImpl service = new InitiativeBlessingServiceImpl();
            scene.applyInitiativeBlessings(sheet -> service.resolveBlessings(sheet.getCharacter()));
            assertEquals(2, one.getTemporaryBonus(ModifierType.ACTION_POINTS));
            assertEquals(2, two.getTemporaryBonus(ModifierType.ACTION_POINTS));
            assertEquals(0, foe.getTemporaryBonus(ModifierType.ACTION_POINTS));
        }
    }

    @Nested
    class InvestidaSelvagem {

        private final ChargeServiceImpl charges = new ChargeServiceImpl();

        private CharacterSheet bruteWith(final Feat... feats) {
            CharacterSheet sheet = sheetWith(feats);
            sheet.getCharacter().grantTitle(new SenhorDaBriga(List.of(), List.of()), TitleSlot.PRIMARY);
            return sheet;
        }

        @Test
        void theWardHoldsOneRdsPerTituloUntilClosed() {
            CharacterSheet charger = bruteWith(MobilidadeFeat.INVESTIDA_SELVAGEM);
            assertTrue(charges.openMovementWard(charger) != null);
            assertEquals(1, charger.getTemporaryBonus(ModifierType.DAMAGE_TAKEN_REDUCTION));
            charges.openMovementWard(charger);
            assertEquals(1, charger.getTemporaryBonus(ModifierType.DAMAGE_TAKEN_REDUCTION));
            charges.closeMovementWard(charger);
            assertEquals(0, charger.getTemporaryBonus(ModifierType.DAMAGE_TAKEN_REDUCTION));
        }

        @Test
        void noWardWithoutTheTalento() {
            CharacterSheet charger = bruteWith();
            assertEquals(null, charges.openMovementWard(charger));
        }

        @Test
        void solarGrantsDefesasForAsManyRodadasAsEnemiesHit() {
            CharacterSheet charger = bruteWith(MobilidadeFeat.INVESTIDA_SELVAGEM, MobilidadeFeat.INVESTIDA_SELVAGEM_SOLAR);
            assertTrue(charges.applySavageAftermath(charger, null, 4, 2).isEmpty());
            List<TemporaryEffect> applied = charges.applySavageAftermath(charger, null, 4, 3);
            assertEquals(1, applied.size());
            // 1 + one Bruto Título.
            assertEquals(2, charger.getTemporaryBonus(ModifierType.DEFESAS));
            assertEquals(4, applied.get(0).getRemainingRounds());
        }

        @Test
        void lunarGrantsRouboDeVidaWidenedByAMediumWeapon() {
            CharacterSheet charger = bruteWith(MobilidadeFeat.INVESTIDA_SELVAGEM, MobilidadeFeat.INVESTIDA_SELVAGEM_LUNAR);
            List<TemporaryEffect> applied = charges.applySavageAftermath(charger,
                    HeavyBladeItem.ESPADA_BASTARDA_OU_KODACHI, 3, 3);
            assertEquals(1, applied.size());
            LifeSteal steal = (LifeSteal) applied.get(0);
            // 1 + one Bruto Título + 1 for a Médio weapon.
            assertEquals(3, steal.getValue());
            assertEquals(3, steal.getRemainingRounds());
        }
    }
}
