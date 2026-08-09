package org.aventyrs.core.scene;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * How far apart two participants in a {@link Scene} are — "Calculando Unidades de Distância
 * (UD)"'s six named bands (e.g. {@code MedicinaECuraExcellency#FOCADO}'s "inimigos próximos
 * (Distância Curta)", {@code AtaqueADistanciaCompetencyAbility#FRIEZA}'s "Distância Curta ou
 * inferior"), plus {@link #AO_ALCANCE_DOS_OLHOS} — not a fixed UD count, but "a distância
 * máxima é limitada à capacidade visual do personagem" per the rules text, so {@link
 * #maxUnidadesDeDistancia} is {@code null} for it alone. Ordered nearest-to-farthest so
 * {@link #isWithin} can compare bands directly — this core has no grid/positioning system to
 * compute a Range from, so it's always supplied already-resolved by a caller, same as {@link
 * InitiativeEntry}'s own {@code initiativeValue} — but {@link #fromUnidadesDeDistancia} lets a
 * caller who *does* track a raw UD count convert instead of resolving the band itself.
 */
@Getter
@AllArgsConstructor
public enum Range {
    ADJACENTE(1),
    DISTANCIA_MUITO_CURTA(2),
    DISTANCIA_CURTA(4),
    DISTANCIA_MEDIA(8),
    DISTANCIA_LONGA(16),
    DISTANCIA_MUITO_LONGA(24),
    AO_ALCANCE_DOS_OLHOS(null);

    /** "Até N Unidades de Distância", or {@code null} for {@link #AO_ALCANCE_DOS_OLHOS}, which has no fixed one. */
    private final Integer maxUnidadesDeDistancia;

    /** Whether this Range is maxRange or nearer — e.g. "Distância Curta ou inferior". */
    public boolean isWithin(final Range maxRange) {
        return this.ordinal() <= maxRange.ordinal();
    }

    /**
     * Which Range a raw Unidades de Distância count falls into, per "Calculando Unidades de
     * Distância" — the nearest band whose {@link #maxUnidadesDeDistancia} still covers it, or
     * {@link #AO_ALCANCE_DOS_OLHOS} beyond {@link #DISTANCIA_MUITO_LONGA}'s 24 UD.
     */
    public static Range fromUnidadesDeDistancia(final int unidadesDeDistancia) {
        for (Range range : values()) {
            if (range.maxUnidadesDeDistancia != null && unidadesDeDistancia <= range.maxUnidadesDeDistancia) {
                return range;
            }
        }
        return AO_ALCANCE_DOS_OLHOS;
    }
}
