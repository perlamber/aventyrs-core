package org.aventyrs.core.scene;

import org.aventyrs.core.sheet.IllegalOperationException;

import static org.aventyrs.core.util.TranslatableMessages.INVALID_AREA_OF_EFFECT;

/**
 * The footprint an Área de Efeito covers — an {@link AreaShape} plus one measurement in
 * Unidades de Distância, the same UD scale {@link Range} is denominated in.
 *
 * <p>One number covers all three shapes, read differently by each:
 *
 * <pre>
 * CIRCULO(2) → a disc of radius 2 UD around its centre
 * LINHA(6)   → a line 6 UD long, radiating outward
 * CONE(4)    → a wedge 4 UD long, radiating outward
 * </pre>
 *
 * <h2>The conventions this class assumes</h2>
 *
 * These are the only assumptions here, and each is revisitable the day a real rules-text block
 * contradicts it:
 *
 * <ul>
 *   <li><b>1 UD is 1 hex step</b> — already established by {@code
 *       org.aventyrs.core.scene.grid.RangeBand}, and the same scale {@link
 *       Range#fromUnidadesDeDistancia} converts on.</li>
 *   <li><b>A {@link AreaShape#LINHA} is 1 UD wide</b>, so its length is its whole description.</li>
 *   <li><b>A {@link AreaShape#CONE}'s spread is derived from its length</b>, not stored — no
 *       angle is modelled.</li>
 * </ul>
 *
 * Adding a width or an angle would mean a shape-specific parameter, and no rules text names one
 * yet; keep the single measurement until one does.
 *
 * <h2>What this deliberately doesn't say</h2>
 *
 * This is the footprint's <em>size and form only</em>. It says nothing about where the area sits
 * (that's {@code org.aventyrs.core.magic.SpellTargeting}'s {@code range}), which direction an
 * emanation points (chosen per use, by whoever aims it), which hexes are actually covered (no
 * footprint resolution exists — see the class-level note on {@code
 * org.aventyrs.core.scene.grid.GridPosition}'s package), or who inside it is spared.
 *
 * <p>It is deliberately not magic-specific: {@code
 * org.aventyrs.core.skill.esquivaeaparar.EsquivaEApararCompetencyAbility#EVASAO} and {@code
 * org.aventyrs.core.title.santo.AbencoadoPelaLuzAbility} both describe Área de Efeito effects
 * that aren't Magias, which is why this lives beside {@link Range} rather than in {@code
 * org.aventyrs.core.magic}.
 */
public record AreaOfEffect(AreaShape shape, int unidadesDeDistancia) {

    /** The smallest area anything can cover — a footprint of zero UD isn't an area at all. */
    public static final int MIN_UNIDADES_DE_DISTANCIA = 1;

    /**
     * Validates the authored footprint, a genuine system boundary — the same treatment {@code
     * org.aventyrs.core.character.DamageBase#of} and {@code
     * org.aventyrs.core.skill.SkillRoll}'s own dice already get. Throws {@link
     * IllegalOperationException} ({@code INVALID_AREA_OF_EFFECT}) on a null shape or a
     * measurement below {@link #MIN_UNIDADES_DE_DISTANCIA}.
     */
    public AreaOfEffect {
        if (shape == null || unidadesDeDistancia < MIN_UNIDADES_DE_DISTANCIA) {
            throw new IllegalOperationException(INVALID_AREA_OF_EFFECT);
        }
    }

    /** A disc of the given radius in UD, around a centre point. */
    public static AreaOfEffect circle(final int radius) {
        return new AreaOfEffect(AreaShape.CIRCULO, radius);
    }

    /** A line of the given length in UD, radiating outward from whoever produced it. */
    public static AreaOfEffect line(final int length) {
        return new AreaOfEffect(AreaShape.LINHA, length);
    }

    /** A cone of the given length in UD, radiating outward from whoever produced it. */
    public static AreaOfEffect cone(final int length) {
        return new AreaOfEffect(AreaShape.CONE, length);
    }

    /** A line of the given length in UD that pierces through what it passes — see {@link AreaShape#PENETRANTE}. */
    public static AreaOfEffect penetrating(final int length) {
        return new AreaOfEffect(AreaShape.PENETRANTE, length);
    }

    /** A burst of the given radius in UD — see {@link AreaShape#EXPLOSAO}. */
    public static AreaOfEffect explosion(final int radius) {
        return new AreaOfEffect(AreaShape.EXPLOSAO, radius);
    }

    /**
     * The same footprint, sized by one of {@link Range}'s named bands rather than a raw UD count.
     * The Magia catalog names every area size with exactly those words — {@code Área Circular
     * Média}, {@code Cone Curto} — so this is the vocabulary an authored area is written in, and
     * no new one had to be invented for it.
     *
     * <p>{@link Range#AO_ALCANCE_DOS_OLHOS} has no fixed UD count and so cannot size an area;
     * passing it throws {@link IllegalOperationException} ({@code INVALID_AREA_OF_EFFECT}), the
     * same treatment every other malformed footprint gets.
     */
    public static AreaOfEffect of(final AreaShape shape, final Range range) {
        Integer unidadesDeDistancia = range == null ? null : range.getMaxUnidadesDeDistancia();
        if (unidadesDeDistancia == null) {
            throw new IllegalOperationException(INVALID_AREA_OF_EFFECT);
        }
        return new AreaOfEffect(shape, unidadesDeDistancia);
    }

    /** A disc whose radius is one of {@link Range}'s bands — see {@link #of(AreaShape, Range)}. */
    public static AreaOfEffect circle(final Range range) {
        return of(AreaShape.CIRCULO, range);
    }

    /** A line whose length is one of {@link Range}'s bands — see {@link #of(AreaShape, Range)}. */
    public static AreaOfEffect line(final Range range) {
        return of(AreaShape.LINHA, range);
    }

    /** A cone whose length is one of {@link Range}'s bands — see {@link #of(AreaShape, Range)}. */
    public static AreaOfEffect cone(final Range range) {
        return of(AreaShape.CONE, range);
    }

    /** A piercing line whose length is one of {@link Range}'s bands — see {@link #of(AreaShape, Range)}. */
    public static AreaOfEffect penetrating(final Range range) {
        return of(AreaShape.PENETRANTE, range);
    }

    /** A burst whose radius is one of {@link Range}'s bands — see {@link #of(AreaShape, Range)}. */
    public static AreaOfEffect explosion(final Range range) {
        return of(AreaShape.EXPLOSAO, range);
    }

    /** Whether this footprint radiates outward from whoever produced it — see {@link AreaShape#isEmanation()}. */
    public boolean isEmanation() {
        return shape.isEmanation();
    }

    /** e.g. {@code "CIRCULO 2UD"}. */
    @Override
    public String toString() {
        return shape + " " + unidadesDeDistancia + "UD";
    }
}
