package org.aventyrs.core.scene;

/**
 * The kind of environment a {@link Scene} is currently taking place in — e.g. {@code
 * AnoesRacialAbility#FILHOS_DA_MONTANHA}'s Vantagem while the terrain is {@link #MOUNTAIN} or
 * {@link #CAVE}. This core has no grid/positioning system of its own (same reasoning as
 * {@link Range}'s own javadoc), so a Scene's terrain is always supplied by a caller — it's one
 * value for the whole Scene, not per-participant, matching how the rules text this models
 * ("terrenos montanhosos ou cavernas") describes a place, not a distance between two people.
 */
public enum TerrainType {
    URBAN,
    CAVE,
    MOUNTAIN,
    FOREST,
    DESERT,
    AQUATIC
}
