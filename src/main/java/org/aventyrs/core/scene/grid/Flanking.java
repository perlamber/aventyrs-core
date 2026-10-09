package org.aventyrs.core.scene.grid;

import lombok.NonNull;
import org.aventyrs.core.character.SizeCategory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Whether a combatant is <b>Flanqueado</b> — "Cercar um personagem exige a presença de 1+ Categoria de
 * Tamanho personagens (mínimo 2) adjacentes, posicionado em direções opostas, triangular, quadrangular
 * ou circular." Pure geometry over positions a caller supplies (this core holds none), so the client can
 * derive the Condição from its board and apply it (table ruling, 2026-10-07).
 *
 * <p>Two tests, both required:
 *
 * <ul>
 *   <li><b>Enough of them</b>: {@link #requiredSurrounders} — 1 + the target's Categoria, never fewer
 *   than 2 (a human needs 2, a Categoria +3 creature 4). Only enemies able to fight count; the caller
 *   leaves out Caído, Imobilizado, Desacordado and Devorado ones (an Agarrado one still counts).</li>
 *   <li><b>Spread around it</b>: read as "no half of the circle around the target is empty" — the
 *   largest angular gap between neighbouring enemies, seen from the target's centre, is at most 180°.
 *   Two enemies exactly opposite pass; two side by side do not; a triangle, a square or a ring pass.
 *   ⚠️ An inference: the text lists shapes, not an angle.</li>
 * </ul>
 *
 * <p>"Adjacente" reaches past a large body: an enemy counts within {@code 1 + bodyRadius} hexes of the
 * target's position ({@link SizeCategory#getBodyRadius()}).
 */
public final class Flanking {

    /** "(mínimo 2)". */
    public static final int MINIMUM_SURROUNDERS = 2;

    private static final double HALF_TURN = Math.PI;
    private static final double EPSILON = 1e-9;

    private Flanking() {
    }

    /** How many able enemies it takes to surround a combatant of size. */
    public static int requiredSurrounders(@NonNull final SizeCategory size) {
        return Math.max(MINIMUM_SURROUNDERS, 1 + size.getCategory());
    }

    /**
     * Whether a combatant of size standing at target is surrounded by ableEnemies — the positions of
     * the enemies able to fight, wherever they stand (those not adjacent are ignored).
     */
    public static boolean isFlanked(@NonNull final GridPosition target, @NonNull final SizeCategory size,
                                    @NonNull final Collection<GridPosition> ableEnemies) {
        int reach = 1 + size.getBodyRadius();
        List<Double> angles = new ArrayList<>();
        for (GridPosition enemy : ableEnemies) {
            int distance = HexGrid.distance(target, enemy);
            if (distance >= 1 && distance <= reach) {
                double[] offset = pixel(enemy, target);
                angles.add(Math.atan2(offset[1], offset[0]));
            }
        }
        if (angles.size() < requiredSurrounders(size)) {
            return false;
        }
        angles.sort(Double::compare);
        double largestGap = 2 * Math.PI - (angles.get(angles.size() - 1) - angles.get(0));
        for (int i = 1; i < angles.size(); i++) {
            largestGap = Math.max(largestGap, angles.get(i) - angles.get(i - 1));
        }
        return largestGap <= HALF_TURN + EPSILON;
    }

    /** Flat-top pixel offset of hex from origin, in hex-width units — the same projection {@link AreaFootprint} uses. */
    private static double[] pixel(final GridPosition hex, final GridPosition origin) {
        double[] a = cube(hex);
        double[] b = cube(origin);
        double q = a[0] - b[0];
        double r = a[2] - b[2];
        return new double[] {1.5 * q, Math.sqrt(3) * (r + q / 2)};
    }

    private static double[] cube(final GridPosition position) {
        double x = position.x();
        double z = position.y() - Math.floorDiv(position.x(), 2);
        return new double[] {x, -x - z, z};
    }
}
