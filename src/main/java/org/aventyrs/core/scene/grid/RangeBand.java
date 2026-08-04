package org.aventyrs.core.scene.grid;

import org.aventyrs.core.scene.Range;

/**
 * Maps a grid's hex-step distance ({@link HexGrid#distance}) onto {@link Range} bands, for a
 * caller building a {@code SceneContext} from grid positions instead of supplying Range
 * directly. Delegates to {@link Range#fromUnidadesDeDistancia}, the rules-text-derived
 * conversion, treating one hex step as one Unidade de Distância — the simplest possible scale,
 * and the one assumption this class makes on the grid's behalf, since neither the grid nor the
 * UD rules text ties a hex to a physical size. Revisit this constant if the grid is meant to
 * represent a coarser or finer scale than 1 UD per hex.
 */
public final class RangeBand {

    private RangeBand() {
    }

    public static Range fromHexDistance(int hexDistance) {
        return Range.fromUnidadesDeDistancia(hexDistance);
    }
}
