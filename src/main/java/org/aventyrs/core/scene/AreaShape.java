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
    CONE(true),

    /**
     * A line that pierces <em>through</em> whatever it passes — the catalog's {@code Área
     * Penetrante}, used by two Magias. Like a {@link #LINHA} it radiates outward from whoever
     * produced it, so it is an emanation and takes no placeable centre; what distinguishes it is
     * that it does not stop at the first target, which only matters once footprint resolution
     * exists.
     */
    PENETRANTE(true),

    /**
     * A burst, the catalog's {@code Explosão} — used by one Magia. Placeable like a {@link
     * #CIRCULO}, and geometrically the same disc today; it is a separate constant because the
     * catalog names it separately and an explosion may well come to differ (cover, falloff)
     * once anything resolves a footprint.
     */
    EXPLOSAO(false);

    private final boolean emanation;

    /**
     * Whether this shape radiates outward from whoever produced it — and therefore has no centre
     * that could be placed at a distance — rather than surrounding a placeable centre point.
     */
    public boolean isEmanation() {
        return emanation;
    }
}
