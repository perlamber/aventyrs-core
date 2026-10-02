package org.aventyrs.core.scene;

import lombok.Getter;
import org.aventyrs.core.sheet.CombatantSheet;

/**
 * An invocation in a {@link Scene} (core 0.0.92) — the summoned sheet, the caster it serves, the group it excludes
 * others of, and how long it has left. See {@link Scene#addSummons}.
 *
 * <p>A Duração of "Concentração + N Rodadas" is two phases ({@code magic.SpellDuration}): while the caster
 * concentrates there is no countdown ({@link #getRemainingRounds()} is {@code null}) and {@link #getTrailingRounds()}
 * holds the N; {@link Scene#breakConcentration} starts it.
 */
@Getter
public final class SceneSummon {

    private final CombatantSheet summon;
    private final CombatantSheet summoner;
    private final String exclusivityGroup;
    private Integer remainingRounds;
    /** The N of "Concentração + N", until the caster's focus breaks; {@code null} once it has, or without one. */
    private Integer trailingRounds;
    private boolean replaced;

    SceneSummon(final CombatantSheet summon, final CombatantSheet summoner, final String exclusivityGroup,
                final Integer remainingRounds, final Integer trailingRounds) {
        this.summon = summon;
        this.summoner = summoner;
        this.exclusivityGroup = exclusivityGroup;
        this.remainingRounds = trailingRounds == null ? remainingRounds : null;
        this.trailingRounds = trailingRounds;
    }

    /** Whether it is held by its caster's Concentração — no countdown has started. */
    public boolean isSustained() {
        return trailingRounds != null;
    }

    /** One Rodada passes; whether its Duração is now spent. */
    boolean tick() {
        if (remainingRounds == null) {
            return false;
        }
        remainingRounds--;
        return remainingRounds <= 0;
    }

    /** The caster's focus broke: the trailing Rodadas begin. Whether it should leave at once (a bare Concentração). */
    boolean release() {
        if (trailingRounds == null) {
            return false;
        }
        remainingRounds = trailingRounds;
        trailingRounds = null;
        return remainingRounds <= 0;
    }

    void markReplaced() {
        replaced = true;
    }
}
