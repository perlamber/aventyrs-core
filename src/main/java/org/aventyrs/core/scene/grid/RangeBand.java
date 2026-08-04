package org.aventyrs.core.scene.grid;

import org.aventyrs.core.scene.Range;

/**
 * Maps a grid's hex-step distance ({@link HexGrid#distance}) onto {@link Range} bands, for a
 * caller building a {@code SceneContext} from grid positions instead of supplying Range
 * directly. This banding is an opt-in convenience, not derived from rules text — treat these
 * thresholds as tunable defaults, not authoritative values.
 */
public final class RangeBand {

    private RangeBand() {
    }

    public static Range fromHexDistance(int hexDistance) {
        if (hexDistance <= 1) {
            return Range.ADJACENTE;
        }
        if (hexDistance <= 2) {
            return Range.DISTANCIA_CURTA;
        }
        if (hexDistance <= 4) {
            return Range.DISTANCIA_MEDIA;
        }
        if (hexDistance <= 8) {
            return Range.DISTANCIA_LONGA;
        }
        return Range.DISTANCIA_MUITO_LONGA;
    }
}
