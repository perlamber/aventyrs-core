package org.aventyrs.core.scene.grid;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Cheapest paths priced by StepRules — geometry only; who may go where is the rules'. */
class MovementPathfinderTest {

    private static final MovementPathfinder BOARD = new MovementPathfinder(10, 10);

    /** Rules from three sets: difficult (cost 2), blocked (neither pass nor stop), passOnly (pass, never stop). */
    private static StepRules rules(final Set<GridPosition> difficult, final Set<GridPosition> blocked,
                                   final Set<GridPosition> passOnly) {
        return new StepRules() {
            @Override
            public boolean canPass(final GridPosition hex) {
                return !blocked.contains(hex);
            }

            @Override
            public boolean canStop(final GridPosition hex) {
                return !blocked.contains(hex) && !passOnly.contains(hex);
            }

            @Override
            public boolean isDifficult(final GridPosition hex) {
                return difficult.contains(hex);
            }

            @Override
            public int enterCost(final GridPosition hex) {
                return difficult.contains(hex) ? 2 : 1;
            }
        };
    }

    private static final StepRules OPEN = rules(Set.of(), Set.of(), Set.of());

    @Test
    void neighboursAgreeWithDistanceAndStayOnTheBoard() {
        GridPosition centre = new GridPosition(5, 5);
        assertEquals(6, HexGrid.neighbours(centre, 10, 10).size());
        HexGrid.neighbours(centre, 10, 10).forEach(n -> assertEquals(1, HexGrid.distance(centre, n)));

        // The corner must not construct (-1, …), whose constructor throws.
        assertTrue(HexGrid.neighbours(new GridPosition(0, 0), 10, 10).size() < 6);
        assertTrue(HexGrid.neighbours(new GridPosition(9, 9), 10, 10).stream()
                .allMatch(n -> n.x() < 10 && n.y() < 10));
    }

    @Test
    void openGroundCostsOneUdPerHex() {
        GridPosition from = new GridPosition(2, 2);
        GridPosition to = new GridPosition(2, 6);

        MovementPath path = BOARD.pathTo(from, to, OPEN).orElseThrow();

        assertEquals(4, path.cost());
        assertEquals(to, path.destination());
        assertFalse(path.touchesDifficultTerrain());
    }

    @Test
    void terrenoDificilCostsTwoUdPerHex() {
        GridPosition from = new GridPosition(0, 0);
        GridPosition to = new GridPosition(0, 3);
        // A wall of difficult terrain across the whole row band the path must cross.
        Set<GridPosition> strip = Set.of(new GridPosition(0, 1), new GridPosition(1, 1), new GridPosition(1, 0),
                new GridPosition(1, 2), new GridPosition(0, 2), new GridPosition(2, 1), new GridPosition(2, 2),
                new GridPosition(2, 0), new GridPosition(3, 0), new GridPosition(3, 1), new GridPosition(3, 2));
        StepRules difficult = rules(strip, Set.of(), Set.of());

        MovementPath path = BOARD.pathTo(from, to, difficult).orElseThrow();

        assertEquals(5, path.cost(), "two difficult hexes at 2UD, then the destination at 1UD");
        assertTrue(path.touchesDifficultTerrain());
    }

    @Test
    void reachableHonoursTheBudgetAndPaysForDifficulty() {
        GridPosition from = new GridPosition(5, 5);
        GridPosition next = new GridPosition(5, 6);

        Map<GridPosition, Integer> open = BOARD.reachable(from, 1, OPEN);
        Map<GridPosition, Integer> hard = BOARD.reachable(from, 1, rules(Set.of(next), Set.of(), Set.of()));

        assertEquals(6, open.size());
        assertTrue(open.containsKey(next));
        assertFalse(hard.containsKey(next), "a 2UD hex is beyond a 1UD budget");
        assertFalse(open.containsKey(from), "the origin is not a destination");
    }

    @Test
    void aHexThatMayBePassedButNotStoppedOnIsOnTheWayButNeverTheEnd() {
        GridPosition from = new GridPosition(0, 0);
        GridPosition ally = new GridPosition(0, 1);
        StepRules rules = rules(Set.of(), Set.of(), Set.of(ally));

        assertTrue(BOARD.pathTo(from, ally, rules).isEmpty());
        assertTrue(BOARD.reachable(from, 3, rules).containsKey(new GridPosition(0, 2)));
        assertFalse(BOARD.reachable(from, 3, rules).containsKey(ally));
    }

    @Test
    void aBlockedHexIsRoutedAroundOrUnreachable() {
        GridPosition from = new GridPosition(0, 0);
        GridPosition to = new GridPosition(0, 2);
        StepRules walled = rules(Set.of(), Set.of(new GridPosition(0, 1)), Set.of());

        MovementPath path = BOARD.pathTo(from, to, walled).orElseThrow();

        assertFalse(path.steps().contains(new GridPosition(0, 1)));
        assertEquals(Optional.empty(), BOARD.pathTo(from, new GridPosition(0, 1), walled));
    }

    @Test
    void avoidingDifficultTerrainRefusesEveryPathThroughIt() {
        GridPosition from = new GridPosition(0, 0);
        GridPosition to = new GridPosition(0, 2);
        Set<GridPosition> everyWay = Set.of(new GridPosition(0, 1), new GridPosition(1, 0), new GridPosition(1, 1));
        StepRules rules = rules(everyWay, Set.of(), Set.of());

        assertTrue(BOARD.pathTo(from, to, rules).isPresent());
        assertTrue(BOARD.pathTo(from, to, rules.avoidingDifficultTerrain()).isEmpty());
    }
}
