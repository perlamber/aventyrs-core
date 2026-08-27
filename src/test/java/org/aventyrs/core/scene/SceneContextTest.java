package org.aventyrs.core.scene;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.sheet.CharacterSheet;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

class SceneContextTest {

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private CharacterSheet newSheet() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();
        return CharacterSheet.of(character, new Player());
    }

    @Test
    void canBeBuiltDirectlyFromPlainListsWithNoScene() {
        CharacterSheet ally = newSheet();
        CharacterSheet enemy = newSheet();

        SceneContext context = new SceneContext(List.of(ally), List.of(enemy), Map.of());

        assertEquals(List.of(ally), context.getAllies());
        assertEquals(List.of(enemy), context.getEnemies());
    }

    @Test
    void buildContextSnapshotsAlliesAndEnemiesFromScene() {
        Scene scene = new Scene();
        UUID heroes = UUID.randomUUID();
        UUID villains = UUID.randomUUID();
        CharacterSheet actor = newSheet();
        CharacterSheet ally = newSheet();
        CharacterSheet enemy = newSheet();
        scene.addParticipant(actor, 10, heroes);
        scene.addParticipant(ally, 8, heroes);
        scene.addParticipant(enemy, 5, villains);

        SceneContext context = scene.buildContext(actor, Map.of());

        assertEquals(Set.of(ally), Set.copyOf(context.getAllies()));
        assertEquals(Set.of(enemy), Set.copyOf(context.getEnemies()));
    }

    @Test
    void getDistanceToReturnsTheSuppliedRangeOrNullIfUnknown() {
        CharacterSheet ally = newSheet();
        CharacterSheet strangerNotTracked = newSheet();

        SceneContext context = new SceneContext(List.of(ally), List.of(), Map.of(ally, Range.ADJACENTE));

        assertEquals(Range.ADJACENTE, context.getDistanceTo(ally));
        assertNull(context.getDistanceTo(strangerNotTracked));
    }

    @Test
    void hasAllyWithinIsTrueWhenAnAllyIsAtOrCloserThanMaxRange() {
        CharacterSheet closeAlly = newSheet();

        SceneContext context = new SceneContext(List.of(closeAlly), List.of(), Map.of(closeAlly, Range.DISTANCIA_CURTA));

        assertTrue(context.hasAllyWithin(Range.DISTANCIA_MEDIA));
        assertTrue(context.hasAllyWithin(Range.DISTANCIA_CURTA));
        assertFalse(context.hasAllyWithin(Range.ADJACENTE));
    }

    @Test
    void hasEnemyWithinIsFalseWhenNoEnemyDistanceIsKnown() {
        CharacterSheet farEnemy = newSheet();

        // farEnemy's distance was never supplied.
        SceneContext context = new SceneContext(List.of(), List.of(farEnemy), Map.of());

        assertFalse(context.hasEnemyWithin(Range.DISTANCIA_MUITO_LONGA));
    }

    @Test
    void hasEnemyWithinReflectsMedicinaECuraFocadoStyleCondition() {
        CharacterSheet nearbyEnemy = newSheet();

        SceneContext context = new SceneContext(List.of(), List.of(nearbyEnemy), Map.of(nearbyEnemy, Range.DISTANCIA_CURTA));

        // MedicinaECuraExcellency.FOCADO's own condition: "não tiver inimigos próximos (Distância Curta)".
        assertTrue(context.hasEnemyWithin(Range.DISTANCIA_CURTA));
    }

    @Test
    void countAlliesWithinCountsOnlyAlliesAtOrCloserThanMaxRange() {
        CharacterSheet adjacentAlly = newSheet();
        CharacterSheet curtaAlly = newSheet();
        CharacterSheet farAlly = newSheet();
        SceneContext context = new SceneContext(
                List.of(adjacentAlly, curtaAlly, farAlly),
                List.of(),
                Map.of(adjacentAlly, Range.ADJACENTE, curtaAlly, Range.DISTANCIA_CURTA, farAlly, Range.DISTANCIA_MUITO_LONGA));

        assertEquals(2, context.countAlliesWithin(Range.DISTANCIA_CURTA));
        assertEquals(1, context.countAlliesWithin(Range.ADJACENTE));
        assertEquals(3, context.countAlliesWithin(Range.DISTANCIA_MUITO_LONGA));
    }

    @Test
    void countAlliesWithinExcludesAlliesWithNoKnownDistance() {
        CharacterSheet trackedAlly = newSheet();
        CharacterSheet untrackedAlly = newSheet();
        SceneContext context = new SceneContext(
                List.of(trackedAlly, untrackedAlly), List.of(), Map.of(trackedAlly, Range.ADJACENTE));

        assertEquals(1, context.countAlliesWithin(Range.DISTANCIA_MUITO_LONGA));
    }

    @Test
    void countEnemiesWithinCountsOnlyEnemiesAtOrCloserThanMaxRange() {
        CharacterSheet adjacentEnemy = newSheet();
        CharacterSheet farEnemy = newSheet();
        SceneContext context = new SceneContext(
                List.of(), List.of(adjacentEnemy, farEnemy),
                Map.of(adjacentEnemy, Range.ADJACENTE, farEnemy, Range.DISTANCIA_LONGA));

        assertEquals(1, context.countEnemiesWithin(Range.ADJACENTE));
        assertEquals(2, context.countEnemiesWithin(Range.DISTANCIA_LONGA));
    }

    @Test
    void getAlliesWithinReturnsOnlyAlliesAtOrCloserThanMaxRange() {
        CharacterSheet adjacentAlly = newSheet();
        CharacterSheet curtaAlly = newSheet();
        CharacterSheet farAlly = newSheet();
        SceneContext context = new SceneContext(
                List.of(adjacentAlly, curtaAlly, farAlly),
                List.of(),
                Map.of(adjacentAlly, Range.ADJACENTE, curtaAlly, Range.DISTANCIA_CURTA, farAlly, Range.DISTANCIA_MUITO_LONGA));

        assertEquals(List.of(adjacentAlly), context.getAlliesWithin(Range.ADJACENTE));
        assertEquals(Set.of(adjacentAlly, curtaAlly), Set.copyOf(context.getAlliesWithin(Range.DISTANCIA_CURTA)));
    }

    @Test
    void getAlliesWithinExcludesAlliesWithNoKnownDistance() {
        CharacterSheet trackedAlly = newSheet();
        CharacterSheet untrackedAlly = newSheet();
        SceneContext context = new SceneContext(
                List.of(trackedAlly, untrackedAlly), List.of(), Map.of(trackedAlly, Range.ADJACENTE));

        assertEquals(List.of(trackedAlly), context.getAlliesWithin(Range.DISTANCIA_MUITO_LONGA));
    }

    @Test
    void getEnemiesWithinReturnsOnlyEnemiesAtOrCloserThanMaxRange() {
        CharacterSheet adjacentEnemy = newSheet();
        CharacterSheet farEnemy = newSheet();
        SceneContext context = new SceneContext(
                List.of(), List.of(adjacentEnemy, farEnemy),
                Map.of(adjacentEnemy, Range.ADJACENTE, farEnemy, Range.DISTANCIA_LONGA));

        assertEquals(List.of(adjacentEnemy), context.getEnemiesWithin(Range.ADJACENTE));
        assertEquals(Set.of(adjacentEnemy, farEnemy), Set.copyOf(context.getEnemiesWithin(Range.DISTANCIA_LONGA)));
    }

    @Test
    void terrainTypeIsNullWhenBuiltFromTheThreeArgConstructor() {
        SceneContext context = new SceneContext(List.of(), List.of(), Map.of());

        assertNull(context.getTerrainType());
    }

    @Test
    void terrainTypeIsCarriedByTheFourArgConstructor() {
        SceneContext context = new SceneContext(List.of(), List.of(), Map.of(), TerrainType.CAVE);

        assertEquals(TerrainType.CAVE, context.getTerrainType());
    }

    @Test
    void isTerrainIsTrueWhenTheCurrentTerrainMatchesOneOfTheGivenValues() {
        SceneContext context = new SceneContext(List.of(), List.of(), Map.of(), TerrainType.MOUNTAIN);

        assertTrue(context.isTerrain(TerrainType.MOUNTAIN, TerrainType.CAVE));
        assertFalse(context.isTerrain(TerrainType.URBAN, TerrainType.AQUATIC));
    }

    @Test
    void isTerrainIsFalseWhenTerrainTypeWasNeverSet() {
        SceneContext context = new SceneContext(List.of(), List.of(), Map.of());

        assertFalse(context.isTerrain(TerrainType.MOUNTAIN, TerrainType.CAVE));
    }

    @Test
    void combatSceneCurrentRoundAndWonInitiativeDefaultToNonCombatWhenBuiltFromTheShorterConstructors() {
        SceneContext threeArg = new SceneContext(List.of(), List.of(), Map.of());
        SceneContext fourArg = new SceneContext(List.of(), List.of(), Map.of(), TerrainType.CAVE);

        assertFalse(threeArg.isCombatScene());
        assertEquals(0, threeArg.getCurrentRound());
        assertFalse(threeArg.hasWonInitiative());
        assertFalse(fourArg.isCombatScene());
        assertEquals(0, fourArg.getCurrentRound());
        assertFalse(fourArg.hasWonInitiative());
    }

    @Test
    void sevenArgConstructorCarriesCombatSceneCurrentRoundAndWonInitiative() {
        SceneContext context = new SceneContext(List.of(), List.of(), Map.of(), null, true, 1, true);

        assertTrue(context.isCombatScene());
        assertEquals(1, context.getCurrentRound());
        assertTrue(context.hasWonInitiative());
    }

    @Test
    void isWithinFirstCombatRoundsIsFalseOutsideACombatScene() {
        SceneContext context = new SceneContext(List.of(), List.of(), Map.of(), null, false, 1, false);

        assertFalse(context.isWithinFirstCombatRounds(2));
    }

    @Test
    void isWithinFirstCombatRoundsExcludesRoundZero() {
        SceneContext context = new SceneContext(List.of(), List.of(), Map.of(), null, true, 0, false);

        assertFalse(context.isWithinFirstCombatRounds(2));
    }

    @Test
    void isWithinFirstCombatRoundsIsTrueForRoundsOneThroughRoundCount() {
        SceneContext roundOne = new SceneContext(List.of(), List.of(), Map.of(), null, true, 1, false);
        SceneContext roundTwo = new SceneContext(List.of(), List.of(), Map.of(), null, true, 2, false);
        SceneContext roundThree = new SceneContext(List.of(), List.of(), Map.of(), null, true, 3, false);

        assertTrue(roundOne.isWithinFirstCombatRounds(2));
        assertTrue(roundTwo.isWithinFirstCombatRounds(2));
        assertFalse(roundThree.isWithinFirstCombatRounds(2));
    }
}
