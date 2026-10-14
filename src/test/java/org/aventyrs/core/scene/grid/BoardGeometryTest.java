package org.aventyrs.core.scene.grid;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Straight lines and three-dimensional distance on the hex board (core 0.1.6.16). */
class BoardGeometryTest {

    @Test
    void aLineIncludesBothEndsAndStepsOneHexAtATime() {
        GridPosition from = new GridPosition(2, 3);
        GridPosition to = new GridPosition(9, 7);

        List<GridPosition> line = HexGrid.line(from, to);

        assertEquals(from, line.get(0));
        assertEquals(to, line.get(line.size() - 1));
        assertEquals(HexGrid.distance(from, to) + 1, line.size());
        for (int i = 1; i < line.size(); i++) {
            assertEquals(1, HexGrid.distance(line.get(i - 1), line.get(i)));
        }
    }

    @Test
    void aLineDownOneColumnStaysInIt() {
        assertEquals(List.of(new GridPosition(4, 1), new GridPosition(4, 2), new GridPosition(4, 3)),
                HexGrid.line(new GridPosition(4, 1), new GridPosition(4, 3)));
    }

    @Test
    void aLineToItselfIsOneHex() {
        assertEquals(List.of(new GridPosition(5, 5)), HexGrid.line(new GridPosition(5, 5), new GridPosition(5, 5)));
    }

    /** reach(a,b) = max(hex(a,b), |za − zb|) — table ruling, 2026-10-14. */
    @Test
    void distanceInThreeDimensionsIsTheLargerAxis() {
        BoardPosition ground = BoardPosition.at(new GridPosition(5, 5));
        assertEquals(3, HexGrid.distance3d(ground, new BoardPosition(new GridPosition(5, 5), 3)));
        assertEquals(4, HexGrid.distance3d(ground, new BoardPosition(new GridPosition(5, 9), 2)));
        assertEquals(1, HexGrid.distance3d(ground, new BoardPosition(new GridPosition(5, 6), -1)));
    }
}
