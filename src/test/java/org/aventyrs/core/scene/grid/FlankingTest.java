package org.aventyrs.core.scene.grid;

import org.aventyrs.core.character.SizeCategory;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Flanqueado from positions — the count and the spread (core 0.1.5). */
class FlankingTest {

    private static final GridPosition CENTRE = new GridPosition(10, 10);

    /** The six neighbours of CENTRE (an even column), going round. */
    private static List<GridPosition> ring() {
        return HexGrid.neighbours(CENTRE, 100, 100);
    }

    @Test
    void aHumanNeedsTwoAndALargeCreatureMore() {
        assertEquals(2, Flanking.requiredSurrounders(SizeCategory.MINUS_TWO));
        assertEquals(2, Flanking.requiredSurrounders(SizeCategory.ZERO));
        assertEquals(2, Flanking.requiredSurrounders(SizeCategory.PLUS_ONE));
        assertEquals(4, Flanking.requiredSurrounders(SizeCategory.PLUS_THREE));
    }

    @Test
    void twoEnemiesOnOppositeSidesFlank() {
        GridPosition north = new GridPosition(10, 9);
        GridPosition south = new GridPosition(10, 11);

        assertTrue(Flanking.isFlanked(CENTRE, SizeCategory.ZERO, List.of(north, south)));
    }

    @Test
    void twoEnemiesSideBySideDoNot() {
        List<GridPosition> neighbours = ring();
        GridPosition first = neighbours.get(0);
        GridPosition beside = neighbours.stream().filter(n -> HexGrid.distance(first, n) == 1).findFirst().orElseThrow();

        assertFalse(Flanking.isFlanked(CENTRE, SizeCategory.ZERO, List.of(first, beside)));
    }

    @Test
    void aTriangleFlanks() {
        // Every other neighbour: three enemies 120° apart.
        List<GridPosition> neighbours = ring();
        GridPosition a = neighbours.get(0);
        List<GridPosition> notAdjacentToA = neighbours.stream()
                .filter(n -> n != a && HexGrid.distance(a, n) == 2).toList();
        GridPosition b = notAdjacentToA.get(0);
        GridPosition c = notAdjacentToA.stream().filter(n -> HexGrid.distance(b, n) == 2).findFirst().orElseThrow();

        assertTrue(Flanking.isFlanked(CENTRE, SizeCategory.ZERO, List.of(a, b, c)));
    }

    @Test
    void oneEnemyIsNeverEnoughAndADistantOneDoesNotCount() {
        assertFalse(Flanking.isFlanked(CENTRE, SizeCategory.ZERO, List.of(new GridPosition(10, 9))));
        assertFalse(Flanking.isFlanked(CENTRE, SizeCategory.ZERO,
                List.of(new GridPosition(10, 9), new GridPosition(10, 13))));
    }

    @Test
    void aLargeCreatureNeedsMoreSurroundersWithinItsBody() {
        // Categoria +3 needs 4; two opposite are not enough.
        assertFalse(Flanking.isFlanked(CENTRE, SizeCategory.PLUS_THREE,
                List.of(new GridPosition(10, 9), new GridPosition(10, 11))));
    }
}
