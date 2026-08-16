package org.aventyrs.core.scene;

import org.aventyrs.core.ability.AttributeAbility;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.character.services.InitiativeBlessingService;
import org.aventyrs.core.character.services.InitiativeBlessingServiceImpl;
import org.aventyrs.core.ego.InitiativeAdvantage;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.InitiativeBlessing;
import org.aventyrs.core.sheet.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SceneTest {

    private final InitiativeBlessingService blessingService = new InitiativeBlessingServiceImpl();

    private static class SelfOnlyBlessingAbility implements AttributeAbility {
        @Override
        public AttributeDomain getAttributeDomain() {
            return AttributeDomain.INSTINCT;
        }

        @Override
        public String getDescription() {
            return "Test-only self-only initiative blessing source.";
        }

        @Override
        public List<InitiativeBlessing> resolveInitiativeBlessings() {
            return List.of(new InitiativeBlessing(ModifierType.REACTIONS, 1, 1, false));
        }
    }

    @BeforeEach
    public void setup() {
        CharacterFixture.loadTemplates();
    }

    private CharacterSheet newSheet() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();
        return CharacterSheet.of(character, new Player());
    }

    private CharacterSheet sheetWithPosicionamentoEstrategico() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .egoAdvantage(EgoDomain.INICIATIVA, InitiativeAdvantage.POSICIONAMENTO_ESTRATEGICO)
                .build();
        return CharacterSheet.of(character, new Player());
    }

    private CharacterSheet sheetWithSelfOnlyBlessing() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributeAbility(new SelfOnlyBlessingAbility())
                .build();
        return CharacterSheet.of(character, new Player());
    }

    private List<InitiativeBlessing> blessingsFor(final CharacterSheet sheet) {
        return blessingService.resolveBlessings(sheet.getCharacter());
    }

    @Test
    void canBeCreatedEmptyWithNoParticipantsYet() {
        Scene scene = new Scene();
        assertTrue(scene.getParticipantsInInitiativeOrder().isEmpty());
    }

    @Test
    void ordersParticipantsByDescendingInitiativeValue() {
        Scene scene = new Scene();
        CharacterSheet slow = newSheet();
        CharacterSheet fast = newSheet();
        CharacterSheet medium = newSheet();

        scene.addParticipant(slow, 5);
        scene.addParticipant(fast, 18);
        scene.addParticipant(medium, 11);

        assertEquals(List.of(fast, medium, slow), scene.getParticipantsInInitiativeOrder());
    }

    @Test
    void addParticipantReturnsTheUpdatedOrder() {
        Scene scene = new Scene();
        CharacterSheet first = newSheet();
        CharacterSheet second = newSheet();

        scene.addParticipant(first, 10);
        List<CharacterSheet> updated = scene.addParticipant(second, 15);

        assertEquals(List.of(second, first), updated);
    }

    @Test
    void tiedInitiativeValuesKeepTheOrderTheyWereAddedIn() {
        Scene scene = new Scene();
        CharacterSheet addedFirst = newSheet();
        CharacterSheet addedSecond = newSheet();

        scene.addParticipant(addedFirst, 10);
        scene.addParticipant(addedSecond, 10);

        assertEquals(List.of(addedFirst, addedSecond), scene.getParticipantsInInitiativeOrder());
    }

    @Test
    void nextThrowsWhenNoParticipantHasBeenAdded() {
        Scene scene = new Scene();
        assertThrows(IllegalOperationException.class, scene::next);
    }

    @Test
    void nextCyclesThroughParticipantsInInitiativeOrderThenWrapsAround() {
        Scene scene = new Scene();
        CharacterSheet slow = newSheet();
        CharacterSheet fast = newSheet();
        CharacterSheet medium = newSheet();
        scene.addParticipant(slow, 5);
        scene.addParticipant(fast, 18);
        scene.addParticipant(medium, 11);

        assertEquals(fast, scene.next());
        assertEquals(medium, scene.next());
        assertEquals(slow, scene.next());
        assertEquals(fast, scene.next());
    }

    @Test
    void currentRoundStartsAtZeroAndIncrementsEveryFullCycle() {
        Scene scene = new Scene();
        CharacterSheet first = newSheet();
        CharacterSheet second = newSheet();
        scene.addParticipant(first, 10);
        scene.addParticipant(second, 5);

        scene.next();
        assertEquals(0, scene.getCurrentRound());
        scene.next();
        assertEquals(0, scene.getCurrentRound());
        scene.next();
        assertEquals(1, scene.getCurrentRound());
    }

    @Test
    void participantAddedMidRoundDoesNotInterruptTheCurrentRound() {
        Scene scene = new Scene();
        CharacterSheet a = newSheet();
        CharacterSheet b = newSheet();
        CharacterSheet lateArrival = newSheet();
        scene.addParticipant(a, 10);
        scene.addParticipant(b, 5);

        assertEquals(a, scene.next());
        scene.addParticipant(lateArrival, 20);
        assertEquals(List.of(a, b), scene.getParticipantsInInitiativeOrder());
        assertEquals(b, scene.next());
    }

    @Test
    void participantAddedMidRoundJoinsAtItsSortedSlotStartingNextRound() {
        Scene scene = new Scene();
        CharacterSheet a = newSheet();
        CharacterSheet b = newSheet();
        CharacterSheet lateArrival = newSheet();
        scene.addParticipant(a, 10);
        scene.addParticipant(b, 5);

        scene.next();
        scene.addParticipant(lateArrival, 20);
        scene.next();

        assertEquals(lateArrival, scene.next());
        assertEquals(1, scene.getCurrentRound());
        assertEquals(List.of(lateArrival, a, b), scene.getParticipantsInInitiativeOrder());
    }

    @Test
    void nextCallsFinishTurnOnTheParticipantWhoseTurnJustEnded() {
        Scene scene = new Scene();
        CharacterSheet a = newSheet();
        CharacterSheet b = newSheet();
        scene.addParticipant(a, 10);
        scene.addParticipant(b, 5);
        a.grantTemporaryBonus(ModifierType.INITIATIVE, 3, 1);
        assertEquals(3, a.getTemporaryBonus(ModifierType.INITIATIVE));

        scene.next();
        assertEquals(3, a.getTemporaryBonus(ModifierType.INITIATIVE));
        scene.next();

        assertEquals(0, a.getTemporaryBonus(ModifierType.INITIATIVE));
    }

    @Test
    void nextDoesNotCallFinishTurnOnTheVeryFirstCall() {
        Scene scene = new Scene();
        CharacterSheet a = newSheet();
        scene.addParticipant(a, 10);
        a.grantTemporaryBonus(ModifierType.INITIATIVE, 3, 1);

        scene.next();

        assertEquals(3, a.getTemporaryBonus(ModifierType.INITIATIVE));
    }

    @Test
    void wonInitiativeReflectsAGrantedInitiativeBonusImmediately() {
        Scene scene = new Scene();
        UUID heroes = UUID.randomUUID();
        UUID villains = UUID.randomUUID();
        CharacterSheet hero = newSheet();
        CharacterSheet villain = newSheet();
        scene.addParticipant(hero, 5, heroes);
        scene.addParticipant(villain, 18, villains);
        assertFalse(scene.wonInitiative(hero));

        // An Ability grants the bonus straight to the CharacterSheet it already holds a
        // reference to — no call into Scene at all.
        hero.grantTemporaryBonus(ModifierType.INITIATIVE, 20, 1);

        assertTrue(scene.wonInitiative(hero));
        assertFalse(scene.wonInitiative(villain));
    }

    @Test
    void turnOrderStaysFrozenMidRoundThenReflectsAGrantedInitiativeBonusAtTheNextRoundBoundary() {
        Scene scene = new Scene();
        CharacterSheet a = newSheet();
        CharacterSheet b = newSheet();
        scene.addParticipant(a, 10);
        scene.addParticipant(b, 5);

        scene.next();
        b.grantTemporaryBonus(ModifierType.INITIATIVE, 20, 2);
        assertEquals(List.of(a, b), scene.getParticipantsInInitiativeOrder());

        scene.next();
        scene.next();

        assertEquals(List.of(b, a), scene.getParticipantsInInitiativeOrder());
    }

    @Test
    void participantAddedMidRoundWithAnActiveInitiativeBonusIsPositionedByItsEffectiveValueOnceMerged() {
        Scene scene = new Scene();
        CharacterSheet a = newSheet();
        CharacterSheet b = newSheet();
        CharacterSheet lateArrival = newSheet();
        lateArrival.grantTemporaryBonus(ModifierType.INITIATIVE, 20, 2);
        scene.addParticipant(a, 10);
        scene.addParticipant(b, 5);

        scene.next();
        scene.addParticipant(lateArrival, 1);
        scene.next();
        scene.next();

        assertEquals(List.of(lateArrival, a, b), scene.getParticipantsInInitiativeOrder());
    }

    @Test
    void participantsAddedWithoutAnExplicitGroupHaveNoAllies() {
        Scene scene = new Scene();
        CharacterSheet a = newSheet();
        CharacterSheet b = newSheet();
        scene.addParticipant(a, 10);
        scene.addParticipant(b, 5);

        assertTrue(scene.getAllies(a).isEmpty());
        assertTrue(scene.getAllies(b).isEmpty());
    }

    @Test
    void participantsSharingAGroupAreEachOthersAllies() {
        Scene scene = new Scene();
        UUID party = UUID.randomUUID();
        CharacterSheet a = newSheet();
        CharacterSheet b = newSheet();
        CharacterSheet c = newSheet();
        scene.addParticipant(a, 10, party);
        scene.addParticipant(b, 5, party);
        scene.addParticipant(c, 8, party);

        // getAllies makes no ordering guarantee, only membership.
        assertEquals(Set.of(b, c), Set.copyOf(scene.getAllies(a)));
    }

    @Test
    void alliesExcludesTheActorItself() {
        Scene scene = new Scene();
        UUID party = UUID.randomUUID();
        CharacterSheet a = newSheet();
        scene.addParticipant(a, 10, party);

        assertTrue(scene.getAllies(a).isEmpty());
    }

    @Test
    void participantsInDifferentGroupsAreNotAllies() {
        Scene scene = new Scene();
        UUID heroes = UUID.randomUUID();
        UUID villains = UUID.randomUUID();
        CharacterSheet hero = newSheet();
        CharacterSheet villain = newSheet();
        scene.addParticipant(hero, 10, heroes);
        scene.addParticipant(villain, 5, villains);

        assertTrue(scene.getAllies(hero).isEmpty());
        assertTrue(scene.getAllies(villain).isEmpty());
    }

    @Test
    void alliesIncludesAPartyMemberAddedMidRound() {
        Scene scene = new Scene();
        UUID party = UUID.randomUUID();
        CharacterSheet a = newSheet();
        CharacterSheet lateArrival = newSheet();
        scene.addParticipant(a, 10, party);

        scene.next();
        scene.addParticipant(lateArrival, 20, party);

        assertEquals(List.of(lateArrival), scene.getAllies(a));
    }

    @Test
    void getAlliesThrowsForACharacterSheetNeverAddedToTheScene() {
        Scene scene = new Scene();
        CharacterSheet stranger = newSheet();

        assertThrows(IllegalOperationException.class, () -> scene.getAllies(stranger));
    }

    @Test
    void getEnemiesReturnsParticipantsInADifferentGroup() {
        Scene scene = new Scene();
        UUID heroes = UUID.randomUUID();
        UUID villains = UUID.randomUUID();
        CharacterSheet hero = newSheet();
        CharacterSheet villainOne = newSheet();
        CharacterSheet villainTwo = newSheet();
        scene.addParticipant(hero, 10, heroes);
        scene.addParticipant(villainOne, 5, villains);
        scene.addParticipant(villainTwo, 8, villains);

        assertEquals(Set.of(villainOne, villainTwo), Set.copyOf(scene.getEnemies(hero)));
    }

    @Test
    void getEnemiesExcludesAllies() {
        Scene scene = new Scene();
        UUID heroes = UUID.randomUUID();
        CharacterSheet a = newSheet();
        CharacterSheet b = newSheet();
        scene.addParticipant(a, 10, heroes);
        scene.addParticipant(b, 5, heroes);

        assertTrue(scene.getEnemies(a).isEmpty());
    }

    @Test
    void participantsAddedWithoutAnExplicitGroupAreEnemiesOfEveryoneElse() {
        Scene scene = new Scene();
        CharacterSheet a = newSheet();
        CharacterSheet b = newSheet();
        scene.addParticipant(a, 10);
        scene.addParticipant(b, 5);

        assertEquals(List.of(b), scene.getEnemies(a));
        assertEquals(List.of(a), scene.getEnemies(b));
    }

    @Test
    void getEnemiesThrowsForACharacterSheetNeverAddedToTheScene() {
        Scene scene = new Scene();
        CharacterSheet stranger = newSheet();

        assertThrows(IllegalOperationException.class, () -> scene.getEnemies(stranger));
    }

    @Test
    void terrainTypeIsUnsetByDefault() {
        Scene scene = new Scene();
        assertNull(scene.getTerrainType());
    }

    @Test
    void terrainTypeCanBeSetAndRead() {
        Scene scene = new Scene();
        scene.setTerrainType(TerrainType.CAVE);

        assertEquals(TerrainType.CAVE, scene.getTerrainType());
    }

    @Test
    void buildContextCarriesTheScenesTerrainTypeIntoTheSnapshot() {
        Scene scene = new Scene();
        scene.setTerrainType(TerrainType.MOUNTAIN);
        CharacterSheet actor = newSheet();
        scene.addParticipant(actor, 10);

        SceneContext context = scene.buildContext(actor, Map.of());

        assertEquals(TerrainType.MOUNTAIN, context.getTerrainType());
    }

    @Test
    void combatSceneIsFalseByDefault() {
        Scene scene = new Scene();
        assertFalse(scene.isCombatScene());
    }

    @Test
    void combatSceneCanBeSetAndRead() {
        Scene scene = new Scene();
        scene.setCombatScene(true);

        assertTrue(scene.isCombatScene());
    }

    @Test
    void buildContextCarriesCombatSceneAndCurrentRoundIntoTheSnapshot() {
        Scene scene = new Scene();
        scene.setCombatScene(true);
        CharacterSheet a = newSheet();
        CharacterSheet b = newSheet();
        scene.addParticipant(a, 10);
        scene.addParticipant(b, 5);
        scene.next();
        scene.next();
        scene.next();

        SceneContext context = scene.buildContext(a, Map.of());

        assertTrue(context.isCombatScene());
        assertEquals(1, context.getCurrentRound());
    }

    @Test
    void wonInitiativeIsTrueForTheGroupHoldingTheHighestValue() {
        Scene scene = new Scene();
        UUID heroes = UUID.randomUUID();
        UUID villains = UUID.randomUUID();
        CharacterSheet fastHero = newSheet();
        CharacterSheet slowHero = newSheet();
        CharacterSheet villain = newSheet();
        scene.addParticipant(fastHero, 18, heroes);
        scene.addParticipant(slowHero, 5, heroes);
        scene.addParticipant(villain, 11, villains);

        assertTrue(scene.wonInitiative(fastHero));
        assertTrue(scene.wonInitiative(slowHero));
        assertFalse(scene.wonInitiative(villain));
    }

    @Test
    void wonInitiativeIsTrueForEveryGroupTiedForTheHighestValue() {
        Scene scene = new Scene();
        UUID heroes = UUID.randomUUID();
        UUID villains = UUID.randomUUID();
        CharacterSheet hero = newSheet();
        CharacterSheet villain = newSheet();
        scene.addParticipant(hero, 10, heroes);
        scene.addParticipant(villain, 10, villains);

        assertTrue(scene.wonInitiative(hero));
        assertTrue(scene.wonInitiative(villain));
    }

    @Test
    void wonInitiativeThrowsForACharacterSheetNeverAddedToTheScene() {
        Scene scene = new Scene();
        CharacterSheet stranger = newSheet();

        assertThrows(IllegalOperationException.class, () -> scene.wonInitiative(stranger));
    }

    @Test
    void buildContextCarriesWonInitiativeIntoTheSnapshot() {
        Scene scene = new Scene();
        UUID heroes = UUID.randomUUID();
        UUID villains = UUID.randomUUID();
        CharacterSheet hero = newSheet();
        CharacterSheet villain = newSheet();
        scene.addParticipant(hero, 18, heroes);
        scene.addParticipant(villain, 5, villains);

        assertTrue(scene.buildContext(hero, Map.of()).hasWonInitiative());
        assertFalse(scene.buildContext(villain, Map.of()).hasWonInitiative());
    }

    @Test
    void applyInitiativeBlessingsThrowsWhenTheCharacterDidNotWinInitiative() {
        Scene scene = new Scene();
        UUID heroes = UUID.randomUUID();
        UUID villains = UUID.randomUUID();
        CharacterSheet hero = sheetWithPosicionamentoEstrategico();
        CharacterSheet villain = newSheet();
        scene.addParticipant(hero, 18, heroes);
        scene.addParticipant(villain, 5, villains);

        assertThrows(IllegalOperationException.class,
                () -> scene.applyInitiativeBlessings(villain, blessingsFor(villain)));
    }

    @Test
    void applyInitiativeBlessingsThrowsForACharacterSheetNeverAddedToTheScene() {
        Scene scene = new Scene();
        CharacterSheet stranger = sheetWithPosicionamentoEstrategico();

        assertThrows(IllegalOperationException.class,
                () -> scene.applyInitiativeBlessings(stranger, blessingsFor(stranger)));
    }

    @Test
    void applyInitiativeBlessingsGrantsAnAllyScopedBlessingToTheWinnerAndItsAllies() {
        Scene scene = new Scene();
        UUID heroes = UUID.randomUUID();
        UUID villains = UUID.randomUUID();
        CharacterSheet winner = sheetWithPosicionamentoEstrategico();
        CharacterSheet ally = newSheet();
        CharacterSheet enemy = newSheet();
        scene.addParticipant(winner, 18, heroes);
        scene.addParticipant(ally, 8, heroes);
        scene.addParticipant(enemy, 5, villains);

        scene.applyInitiativeBlessings(winner, blessingsFor(winner));

        assertEquals(2, winner.getTemporaryBonus(ModifierType.MOVEMENT));
        assertEquals(2, ally.getTemporaryBonus(ModifierType.MOVEMENT));
        assertEquals(0, enemy.getTemporaryBonus(ModifierType.MOVEMENT));
    }

    @Test
    void applyInitiativeBlessingsGrantsASelfOnlyBlessingOnlyToTheWinner() {
        Scene scene = new Scene();
        UUID heroes = UUID.randomUUID();
        CharacterSheet winner = sheetWithSelfOnlyBlessing();
        CharacterSheet ally = newSheet();
        scene.addParticipant(winner, 18, heroes);
        scene.addParticipant(ally, 8, heroes);

        scene.applyInitiativeBlessings(winner, blessingsFor(winner));

        assertEquals(1, winner.getTemporaryBonus(ModifierType.REACTIONS));
        assertEquals(0, ally.getTemporaryBonus(ModifierType.REACTIONS));
    }

    @Test
    void applyInitiativeBlessingsRevokesThePreviousWinnersGrantWhenANewGroupWins() {
        Scene scene = new Scene();
        UUID heroes = UUID.randomUUID();
        UUID villains = UUID.randomUUID();
        CharacterSheet hero = sheetWithPosicionamentoEstrategico();
        CharacterSheet villain = sheetWithPosicionamentoEstrategico();
        scene.addParticipant(hero, 18, heroes);
        scene.addParticipant(villain, 5, villains);
        scene.applyInitiativeBlessings(hero, blessingsFor(hero));
        assertEquals(2, hero.getTemporaryBonus(ModifierType.MOVEMENT));

        // A new, higher-rolling villain joins, flipping who currently wins initiative.
        CharacterSheet strongerVillain = newSheet();
        scene.addParticipant(strongerVillain, 25, villains);
        scene.applyInitiativeBlessings(strongerVillain, blessingsFor(strongerVillain));

        assertEquals(0, hero.getTemporaryBonus(ModifierType.MOVEMENT));
        assertEquals(0, villain.getTemporaryBonus(ModifierType.MOVEMENT));
    }

    @Test
    void addParticipantAppliesActiveAllyScopedBlessingsToANewcomerJoiningTheBlessedGroup() {
        Scene scene = new Scene();
        UUID heroes = UUID.randomUUID();
        CharacterSheet winner = sheetWithPosicionamentoEstrategico();
        scene.addParticipant(winner, 18, heroes);
        scene.applyInitiativeBlessings(winner, blessingsFor(winner));

        CharacterSheet lateAlly = newSheet();
        scene.addParticipant(lateAlly, 3, heroes);

        assertEquals(2, lateAlly.getTemporaryBonus(ModifierType.MOVEMENT));
    }

    @Test
    void addParticipantDoesNotApplyBlessingsToANewcomerJoiningADifferentGroup() {
        Scene scene = new Scene();
        UUID heroes = UUID.randomUUID();
        UUID villains = UUID.randomUUID();
        CharacterSheet winner = sheetWithPosicionamentoEstrategico();
        scene.addParticipant(winner, 18, heroes);
        scene.applyInitiativeBlessings(winner, blessingsFor(winner));

        CharacterSheet lateEnemy = newSheet();
        scene.addParticipant(lateEnemy, 3, villains);

        assertEquals(0, lateEnemy.getTemporaryBonus(ModifierType.MOVEMENT));
    }

    @Test
    void addParticipantWithTheTwoArgOverloadNeverMatchesTheBlessedGroup() {
        Scene scene = new Scene();
        UUID heroes = UUID.randomUUID();
        CharacterSheet winner = sheetWithPosicionamentoEstrategico();
        scene.addParticipant(winner, 18, heroes);
        scene.applyInitiativeBlessings(winner, blessingsFor(winner));

        CharacterSheet lone = newSheet();
        scene.addParticipant(lone, 3);

        assertEquals(0, lone.getTemporaryBonus(ModifierType.MOVEMENT));
    }

    @Test
    void addParticipantDoesNotApplyASelfOnlyBlessingToANewcomerJoiningTheBlessedGroup() {
        Scene scene = new Scene();
        UUID heroes = UUID.randomUUID();
        CharacterSheet winner = sheetWithSelfOnlyBlessing();
        scene.addParticipant(winner, 18, heroes);
        scene.applyInitiativeBlessings(winner, blessingsFor(winner));

        CharacterSheet lateAlly = newSheet();
        scene.addParticipant(lateAlly, 3, heroes);

        assertEquals(0, lateAlly.getTemporaryBonus(ModifierType.REACTIONS));
    }
}
