package org.aventyrs.core.scene.grid;

import org.aventyrs.core.scene.AreaOfEffect;
import org.aventyrs.core.sheet.CombatantSheet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Which hexes an {@link AreaOfEffect} covers once it is placed or aimed, and who stands in them —
 * the footprint resolution {@link AreaOfEffect} itself deliberately leaves out. Positions stay the
 * caller's: it supplies the origin, the aim and the {@link MovementMap}, and this answers.
 *
 * <ul>
 *   <li>{@code CIRCULO}/{@code EXPLOSAO} — every hex within the radius of {@code origin}, the
 *       centre included.</li>
 *   <li>{@code LINHA}/{@code PENETRANTE} — the 1 UD wide hex line from {@code origin} toward
 *       {@code aim}, {@code length} steps long, the origin itself excluded (an emanation starts
 *       from whoever produced it).</li>
 *   <li>{@code CONE} — every hex within {@code length} of {@code origin} whose direction lies
 *       within 30° of the aim, the origin excluded. ⚠️ The 60° spread is an <b>inference</b>:
 *       {@link AreaOfEffect} says a cone's spread "is derived from its length" and no rules text
 *       gives an angle; a hex grid's natural wedge is one of its six 60° sectors.</li>
 * </ul>
 *
 * <p>Nothing here excludes the caster: "a Conjurador is never damaged by their own Magia" is a
 * targeting rule, applied by passing them in {@code excluded} to {@link #occupants}.
 */
public final class AreaFootprint {

    /** Half the cone's spread, in degrees; a small slack keeps hexes exactly on the edge inside. */
    private static final double CONE_HALF_ANGLE_DEGREES = 30.0 + 1e-6;

    private AreaFootprint() {
    }

    /**
     * The hexes area covers on a columns by rows board.
     *
     * @param origin the circle's centre, or where an emanation starts
     * @param aim    where an emanation points; ignored for a circle, required (and different from
     *               origin) for a line or a cone
     */
    public static Set<GridPosition> covering(final AreaOfEffect area, final GridPosition origin,
                                             final GridPosition aim, final int columns, final int rows) {
        int size = area.unidadesDeDistancia();
        return switch (area.shape()) {
            case CIRCULO, EXPLOSAO -> within(origin, size, columns, rows);
            case LINHA, PENETRANTE -> line(origin, requireAim(origin, aim), size, columns, rows);
            case CONE -> cone(origin, requireAim(origin, aim), size, columns, rows);
        };
    }

    /**
     * Everyone standing in hexes on map, less anyone in excluded (matched by sheet id) — the
     * caster, for a Magia that spares its own Conjurador. In board order, each sheet once.
     */
    public static List<CombatantSheet> occupants(final Set<GridPosition> hexes, final MovementMap map,
                                                 final Collection<CombatantSheet> excluded) {
        Set<java.util.UUID> skip = new java.util.HashSet<>();
        excluded.forEach(sheet -> skip.add(sheet.getId()));
        List<CombatantSheet> found = new ArrayList<>();
        for (GridPosition hex : hexes) {
            for (CombatantSheet occupant : map.occupantsOf(hex, null)) {
                if (skip.add(occupant.getId())) {
                    found.add(occupant);
                }
            }
        }
        return found;
    }

    private static GridPosition requireAim(final GridPosition origin, final GridPosition aim) {
        if (aim == null || aim.equals(origin)) {
            throw new IllegalArgumentException("A line or a cone needs an aim other than its origin");
        }
        return aim;
    }

    private static Set<GridPosition> within(final GridPosition centre, final int radius,
                                            final int columns, final int rows) {
        Set<GridPosition> hexes = new LinkedHashSet<>();
        for (GridPosition hex : board(centre, radius, columns, rows)) {
            if (HexGrid.distance(centre, hex) <= radius) {
                hexes.add(hex);
            }
        }
        return hexes;
    }

    /** Cube-coordinate line drawing, extended to length steps along origin → aim. */
    private static Set<GridPosition> line(final GridPosition origin, final GridPosition aim, final int length,
                                          final int columns, final int rows) {
        double[] from = cube(origin);
        double[] toward = cube(aim);
        double distance = HexGrid.distance(origin, aim);
        double[] end = new double[3];
        for (int axis = 0; axis < 3; axis++) {
            end[axis] = from[axis] + (toward[axis] - from[axis]) * length / distance;
        }
        Set<GridPosition> hexes = new LinkedHashSet<>();
        for (int step = 1; step <= length; step++) {
            double t = (double) step / length;
            double[] point = new double[3];
            for (int axis = 0; axis < 3; axis++) {
                // A tiny nudge breaks ties on hex edges consistently, the standard line-draw trick.
                point[axis] = from[axis] + (end[axis] - from[axis]) * t + 1e-6 * (axis + 1);
            }
            GridPosition hex = fromCube(round(point));
            if (hex != null && hex.x() < columns && hex.y() < rows) {
                hexes.add(hex);
            }
        }
        return hexes;
    }

    private static Set<GridPosition> cone(final GridPosition origin, final GridPosition aim, final int length,
                                          final int columns, final int rows) {
        double[] direction = pixel(aim, origin);
        Set<GridPosition> hexes = new LinkedHashSet<>();
        for (GridPosition hex : board(origin, length, columns, rows)) {
            if (hex.equals(origin) || HexGrid.distance(origin, hex) > length) {
                continue;
            }
            double[] offset = pixel(hex, origin);
            double cos = (offset[0] * direction[0] + offset[1] * direction[1])
                    / (Math.hypot(offset[0], offset[1]) * Math.hypot(direction[0], direction[1]));
            if (Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, cos)))) <= CONE_HALF_ANGLE_DEGREES) {
                hexes.add(hex);
            }
        }
        return hexes;
    }

    /** Every on-board hex in the square of the given reach around centre — a superset to filter. */
    private static List<GridPosition> board(final GridPosition centre, final int reach,
                                            final int columns, final int rows) {
        int maxX = Math.min(columns, GridPosition.GRID_SIZE);
        int maxY = Math.min(rows, GridPosition.GRID_SIZE);
        List<GridPosition> hexes = new ArrayList<>();
        for (int x = Math.max(0, centre.x() - reach); x <= Math.min(maxX - 1, centre.x() + reach); x++) {
            for (int y = Math.max(0, centre.y() - reach - 1); y <= Math.min(maxY - 1, centre.y() + reach + 1); y++) {
                hexes.add(new GridPosition(x, y));
            }
        }
        return hexes;
    }

    /** Flat-top pixel offset of hex from origin, in hex-width units — for the cone's angle only. */
    private static double[] pixel(final GridPosition hex, final GridPosition origin) {
        double[] a = cube(hex);
        double[] b = cube(origin);
        double q = a[0] - b[0];
        double r = a[2] - b[2];
        return new double[] {1.5 * q, Math.sqrt(3) * (r + q / 2)};
    }

    /** The same even-q offset → cube conversion {@link HexGrid} uses. */
    private static double[] cube(final GridPosition position) {
        double x = position.x();
        double z = position.y() - Math.floorDiv(position.x(), 2);
        return new double[] {x, -x - z, z};
    }

    private static int[] round(final double[] cube) {
        long rx = Math.round(cube[0]);
        long ry = Math.round(cube[1]);
        long rz = Math.round(cube[2]);
        double dx = Math.abs(rx - cube[0]);
        double dy = Math.abs(ry - cube[1]);
        double dz = Math.abs(rz - cube[2]);
        if (dx > dy && dx > dz) {
            rx = -ry - rz;
        } else if (dy > dz) {
            ry = -rx - rz;
        } else {
            rz = -rx - ry;
        }
        return new int[] {(int) rx, (int) ry, (int) rz};
    }

    /** Back to even-q offset, or {@code null} off the positive board. */
    private static GridPosition fromCube(final int[] cube) {
        int x = cube[0];
        int y = cube[2] + Math.floorDiv(x, 2);
        if (x < 0 || y < 0 || x >= GridPosition.GRID_SIZE || y >= GridPosition.GRID_SIZE) {
            return null;
        }
        return new GridPosition(x, y);
    }
}
