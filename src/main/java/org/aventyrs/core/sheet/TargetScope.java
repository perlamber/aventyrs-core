package org.aventyrs.core.sheet;

/**
 * Who a granted {@link InteractionResult#temporaryBonusValue} applies to — e.g. {@code
 * ArtesCompetencyAbility#DOM_BARDICO}'s {@link #ALLIES}. This is a fixed property of *which
 * ability* granted the bonus, not something this core resolves into an actual recipient list
 * itself — a caller still does that via {@code org.aventyrs.core.scene.Scene#getAllies}/
 * {@code #getEnemies} (for {@link #ALLIES}/{@link #ENEMIES}) or its own target lookup (for
 * {@link #SINGLE_TARGET}).
 */
public enum TargetScope {
    SINGLE_TARGET,
    ALLIES,
    ENEMIES
}
