package org.aventyrs.core.magic;

/**
 * How a Magia reaches whatever it affects — the discriminator {@link SpellTargeting} pairs with
 * the parameters each constant actually takes. This enum alone never describes a Magia's reach
 * fully: three of its four constants need a {@code Range}, an {@code AreaOfEffect}, or both, and
 * an enum constant has nowhere to put them.
 *
 * <pre>
 * PESSOAL         no range, no area
 * TOQUE           no range (Adjacente is implied), no area
 * DISTANCIA       range required, no area
 * AREA_DE_EFEITO  area required, range optional (absent = centred on the Conjurador)
 * </pre>
 *
 * <p><b>{@link #PESSOAL} and {@link #AREA_DE_EFEITO} are the pair most easily confused.</b>
 * {@code PESSOAL} is strictly single-target-on-self — a Magia that simply cannot be cast at
 * anyone else. A burst <em>centred</em> on the Conjurador is an {@code AREA_DE_EFEITO} with no
 * range, not a {@code PESSOAL}: it affects an area, and only where that area originates differs
 * from the ranged case.
 */
public enum SpellReach {

    /** Affects only the Conjurador. Some Magias can be cast nowhere else. */
    PESSOAL,

    /** A single target the Conjurador must touch — Adjacente, delivered by Ataque Corpo-a-Corpo. */
    TOQUE,

    /** An area, either centred on the Conjurador or with its centre placed within a {@code Range}. */
    AREA_DE_EFEITO,

    /** A single target at a distance, up to the {@code Range} the Magia states. */
    DISTANCIA,

    /**
     * Reaches somewhere no {@code Range} can measure — another plane. Two Magias are authored this
     * way, both in the Transporte tree: {@code Alcance: Mesmo Plano} (Portal) and {@code Alcance:
     * Planos Elementais} (Portal Planar).
     *
     * <p>It takes no parameters, exactly as {@link #PESSOAL} and {@link #TOQUE} do, and for a
     * blunter reason: <b>this core has no planar concept at all</b> — no plane exists as a value,
     * so there is nothing to name as a destination and no distance to state. The constant is a
     * classification and nothing more, kept so the two Magias can state their reach at all rather
     * than leaving it {@code null}. Which plane each reaches stays in its own prose until planes
     * are modelled.
     */
    PLANAR
}
