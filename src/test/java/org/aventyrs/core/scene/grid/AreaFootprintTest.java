package org.aventyrs.core.scene.grid;

import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.scene.AreaOfEffect;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Which hexes an Área de Efeito covers, and who stands in them. */
class AreaFootprintTest {

    private static final int BOARD = 20;
    private static final GridPosition CENTRE = new GridPosition(10, 10);

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static Set<GridPosition> cover(final AreaOfEffect area, final GridPosition aim) {
        return AreaFootprint.covering(area, CENTRE, aim, BOARD, BOARD);
    }

    private static GridPosition aNeighbour() {
        return HexGrid.neighbours(CENTRE, BOARD, BOARD).get(0);
    }

    @Test
    void aCircleOfOneIsTheCentreAndItsSixNeighbours() {
        Set<GridPosition> hexes = cover(AreaOfEffect.circle(1), null);

        assertEquals(7, hexes.size());
        assertTrue(hexes.contains(CENTRE));
        assertTrue(hexes.containsAll(HexGrid.neighbours(CENTRE, BOARD, BOARD)));
    }

    /** A hex-grid disc of radius r holds 1 + 3r(r+1) hexes. */
    @Test
    void aCircleOfTwoHoldsNineteenHexes() {
        assertEquals(19, cover(AreaOfEffect.circle(2), null).size());
        assertEquals(19, cover(AreaOfEffect.explosion(2), null).size());
    }

    @Test
    void aCircleIsClippedAtTheBoardsEdge() {
        GridPosition corner = new GridPosition(0, 0);

        assertEquals(1 + HexGrid.neighbours(corner, BOARD, BOARD).size(),
                AreaFootprint.covering(AreaOfEffect.circle(1), corner, null, BOARD, BOARD).size());
    }

    @Test
    void aLineIsOneHexPerStepAndLeavesTheOriginOut() {
        Set<GridPosition> hexes = cover(AreaOfEffect.line(4), aNeighbour());

        assertEquals(4, hexes.size());
        assertFalse(hexes.contains(CENTRE));
        List<Integer> distances = hexes.stream().map(hex -> HexGrid.distance(CENTRE, hex)).toList();
        assertEquals(List.of(1, 2, 3, 4), distances);
        assertTrue(hexes.contains(aNeighbour()));
    }

    /** ⚠️ A 60° wedge: 1 hex at the first step, 3 at the second. */
    @Test
    void aConeWidensWithItsLength() {
        Set<GridPosition> cone = cover(AreaOfEffect.cone(2), aNeighbour());

        assertEquals(4, cone.size());
        assertFalse(cone.contains(CENTRE));
        assertTrue(cone.contains(aNeighbour()));
        assertTrue(cone.stream().allMatch(hex -> HexGrid.distance(CENTRE, hex) <= 2));
    }

    @Test
    void anEmanationNeedsAnAim() {
        assertThrows(IllegalArgumentException.class, () -> cover(AreaOfEffect.line(3), null));
        assertThrows(IllegalArgumentException.class, () -> cover(AreaOfEffect.cone(3), CENTRE));
    }

    @Test
    void occupantsAreEveryoneInsideLessTheExcluded() {
        CombatantSheet caster = CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK).build(), new Player());
        CombatantSheet near = CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK).build(), new Player());
        CombatantSheet far = CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK).build(), new Player());
        MovementMap map = new MovementMap(BOARD, BOARD, Set.of(), Map.of(
                CENTRE, List.of(caster),
                aNeighbour(), List.of(near),
                new GridPosition(15, 15), List.of(far)));

        List<CombatantSheet> hit = AreaFootprint.occupants(cover(AreaOfEffect.circle(1), null), map, List.of(caster));

        assertEquals(List.of(near), hit);
    }
}
