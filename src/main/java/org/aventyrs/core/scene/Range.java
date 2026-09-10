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
 * {@link #isWithin} can compare bands directly. A Range is supplied already-resolved by a
 * caller, same as {@link InitiativeEntry}'s own {@code initiativeValue} — nothing here derives
 * one from where the participants actually stand. Two conversions exist for a caller who tracks
 * position itself: {@link #fromUnidadesDeDistancia} from a raw UD count, and {@code
 * org.aventyrs.core.scene.grid.RangeBand#fromHexDistance} from {@code
 * org.aventyrs.core.scene.grid.HexGrid}'s hex-step distance. The grid is a real, tested
 * facility, but it is one this core offers a caller rather than one {@code SceneContext} runs:
 * no participant carries a {@code GridPosition}, so positions never become distances here.
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
     * This Range widened by {@code steps} bands up the nearest-to-farthest ladder —
     * {@code DISTANCIA_CURTA.increasedBy(1)} is {@code DISTANCIA_MEDIA}. Clamps at both ends:
     * never past {@link #AO_ALCANCE_DOS_OLHOS}, and never before {@link #ADJACENTE} for a
     * negative {@code steps}. This is how {@code
     * org.aventyrs.core.character.services.AttackRangeService} applies a Talento's "+N níveis de
     * distância" to a weapon's or Magia's authored Alcance — a step is a whole band, not a UD
     * count, so nothing here touches {@link #maxUnidadesDeDistancia}.
     */
    public Range increasedBy(final int steps) {
        Range[] all = values();
        return all[Math.min(all.length - 1, Math.max(0, this.ordinal() + steps))];
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
