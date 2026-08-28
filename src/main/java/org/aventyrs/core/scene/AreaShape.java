package org.aventyrs.core.scene;

import lombok.AllArgsConstructor;

/**
 * The footprint an {@link AreaOfEffect} covers — a circle, a line, or a cone.
 *
 * <p>The distinction that earns this enum a field of its own is {@link #isEmanation()}: a
 * {@link #LINHA} and a {@link #CONE} both radiate outward <em>from whoever produced them</em>,
 * so they have no centre anyone could place somewhere else, whereas a {@link #CIRCULO} does.
 * That's why {@code org.aventyrs.core.magic.SpellTargeting} refuses to pair an emanation with a
 * {@code Range} — see its own javadoc. Stored on the constant rather than switched on at the use
 * site, the same way {@link Range} stores {@code maxUnidadesDeDistancia}.
 */
@AllArgsConstructor
public enum AreaShape {

    /** A disc around a centre point, which may be placed away from whoever produced it. */
    CIRCULO(false),

    /** A 1 UD wide line radiating outward from whoever produced it. */
    LINHA(true),

    /** A wedge radiating outward from whoever produced it, widening with its length. */
    CONE(true);

    private final boolean emanation;

    /**
     * Whether this shape radiates outward from whoever produced it — and therefore has no centre
     * that could be placed at a distance — rather than surrounding a placeable centre point.
     */
    public boolean isEmanation() {
        return emanation;
    }
}
