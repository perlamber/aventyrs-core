package org.aventyrs.core.scene;

/**
 * How far apart two participants in a {@link Scene} are — the five distance bands rules text
 * refers to (e.g. {@code MedicinaECuraExcellency#FOCADO}'s "inimigos próximos (Distância
 * Curta)", {@code AtaqueADistanciaCompetencyAbility#FRIEZA}'s "Distância Curta ou inferior").
 * Ordered nearest-to-farthest so {@link #isWithin} can compare bands directly — this core has
 * no grid/positioning system to compute a Range from, so it's always supplied already-resolved
 * by a caller, same as {@link InitiativeEntry}'s own {@code initiativeValue}.
 */
public enum Range {
    ADJACENTE,
    DISTANCIA_CURTA,
    DISTANCIA_MEDIA,
    DISTANCIA_LONGA,
    DISTANCIA_MUITO_LONGA;

    /** Whether this Range is maxRange or nearer — e.g. "Distância Curta ou inferior". */
    public boolean isWithin(final Range maxRange) {
        return this.ordinal() <= maxRange.ordinal();
    }
}
