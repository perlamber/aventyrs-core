package org.aventyrs.core.character.services;

import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.scene.grid.GridPosition;
import org.aventyrs.core.scene.grid.MovementMap;
import org.aventyrs.core.scene.grid.MovementPathfinder;
import org.aventyrs.core.scene.grid.StepRules;
import org.aventyrs.core.scene.grid.TerrainCell;
import org.aventyrs.core.scene.grid.TerrainKind;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Painted terrain cells and travelling at a height (core 0.1.6.16; table ruling 2026-10-14). */
class MovementTerrainCellsTest {

    private final MovementTerrainService service = new MovementTerrainServiceImpl();
    private final MovementPathfinder pathfinder = new MovementPathfinder(10, 10);
    private CharacterSheet walker;

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        walker = CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK).id(UUID.randomUUID()).build(),
                new Player());
    }

    private static MovementMap map(final Map<GridPosition, TerrainCell> cells) {
        return new MovementMap(10, 10, Set.of(), Map.of(), Set.of(), cells);
    }

    private static TerrainCell cell(final TerrainKind kind, final int level) {
        return new TerrainCell(kind, false, level, null);
    }

    @Test
    void anObstacleClosesItsHexToAWalker() {
        GridPosition wall = new GridPosition(3, 3);
        StepRules rules = service.stepRules(walker, map(Map.of(wall, cell(TerrainKind.OBSTACLE, 2))), null, 0);

        assertFalse(rules.canPass(wall));
        assertFalse(rules.canStop(wall));
    }

    @Test
    void waterNeedsANatacaoMovimentoBase() {
        GridPosition pond = new GridPosition(3, 3);
        StepRules rules = service.stepRules(walker, map(Map.of(pond, cell(TerrainKind.WATER, 0))), null, 0);

        assertFalse(rules.canStop(pond));
    }

    @Test
    void aWalkerStepsAtMostOneUdUpOrDown() {
        GridPosition low = new GridPosition(3, 3);
        GridPosition ledge = new GridPosition(3, 4);
        GridPosition cliff = new GridPosition(3, 5);
        StepRules rules = service.stepRules(walker, map(Map.of(ledge, cell(TerrainKind.GROUND, 1),
                cliff, cell(TerrainKind.GROUND, 3))), null, 0);

        assertTrue(rules.canStep(low, ledge));
        assertFalse(rules.canStep(ledge, cliff));
        assertFalse(rules.canStep(cliff, ledge), "a 2UD drop is a fall, not a step");
    }

    @Test
    void aFlyerAboveAnObstaclePassesItButNotOneTallerThanItsFlight() {
        GridPosition wall = new GridPosition(3, 3);
        MovementMap board = map(Map.of(wall, cell(TerrainKind.OBSTACLE, 2)));

        assertTrue(service.stepRules(walker, board, null, 0, 3).canPass(wall));
        assertFalse(service.stepRules(walker, board, null, 0, 2).canPass(wall));
    }

    @Test
    void aFlyerIgnoresTerrenoDificilCost() {
        GridPosition mud = new GridPosition(3, 3);
        MovementMap board = map(Map.of(mud, new TerrainCell(TerrainKind.GROUND, true, 0, null)));

        assertEquals(2, service.stepRules(walker, board, null, 0).enterCost(mud));
        assertEquals(1, service.stepRules(walker, board, null, 0, 2).enterCost(mud));
    }

    @Test
    void aPathGoesAroundAWall() {
        GridPosition from = new GridPosition(2, 2);
        GridPosition to = new GridPosition(2, 4);
        MovementMap board = map(Map.of(new GridPosition(2, 3), cell(TerrainKind.OBSTACLE, 5)));

        var path = pathfinder.pathTo(from, to, service.stepRules(walker, board, null, 0)).orElseThrow();

        assertFalse(path.steps().contains(new GridPosition(2, 3)));
        assertEquals(3, path.cost());
    }

    @Test
    void aStraightLineIsBlockedByAnObstacleOrAnOccupant() {
        GridPosition from = new GridPosition(1, 1);
        GridPosition to = new GridPosition(1, 5);
        assertTrue(map(Map.of()).isLineClear(from, to, walker));
        assertFalse(map(Map.of(new GridPosition(1, 3), cell(TerrainKind.OBSTACLE, 1))).isLineClear(from, to, walker));

        CharacterSheet other = CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK).id(UUID.randomUUID())
                .build(), new Player());
        MovementMap crowded = new MovementMap(10, 10, Set.of(), Map.of(new GridPosition(1, 2), List.of(other)),
                Set.of(), Map.of());
        assertFalse(crowded.isLineClear(from, to, walker));
    }
}
