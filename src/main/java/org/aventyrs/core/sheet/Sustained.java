package org.aventyrs.core.sheet;

import java.util.UUID;

/**
 * An effect held by its caster's Concentração (core 0.0.94) — "Duração: Concentração + N Rodadas". It has no
 * countdown while the caster concentrates; {@link CombatantSheet#releaseSustainedBy} starts the trailing N when their
 * focus breaks (they cast another Magia or attack), which {@code scene.Scene#breakConcentration} does for everyone.
 */
public interface Sustained {

    /** The {@link CombatantSheet#getId()} of whoever concentrates on it. */
    UUID getSustainerId();

    /** The N of "Concentração + N", or {@code null} once the countdown has started. */
    Integer getTrailingRounds();
}
