package org.aventyrs.core.sheet;

import java.util.UUID;

/**
 * An effect held by its caster's Concentração (core 0.0.94) — "Duração: Concentração + N Rodadas". It has no
 * countdown while the caster concentrates; {@link CombatantSheet#releaseSustainedBy} starts the trailing N when their
 * focus is lost (damage taken or an unpaid upkeep — {@link CombatantSheet#loseConcentration()}), which
 * {@code scene.Scene#settleConcentration} does for everyone. See {@code magic.SpellDuration} for the rule.
 */
public interface Sustained {

    /** The {@link CombatantSheet#getId()} of whoever concentrates on it. */
    UUID getSustainerId();

    /** The N of "Concentração + N", or {@code null} once the countdown has started. */
    Integer getTrailingRounds();

    /** The focus broke: starts the trailing countdown. Whether it ends at once — no trailing Rodadas. */
    boolean release();
}
