package org.aventyrs.core.scene;

import lombok.Getter;
import org.aventyrs.core.sheet.CombatantSheet;

import java.util.function.Supplier;

/**
 * Something that invokes a new creature for its caster at each Rodada boundary while it lasts — Totem de Gaea's "A
 * cada Rodada um novo animal é criado desta forma" (core 0.0.92). See {@link Scene#addSummonSpawner}.
 */
@Getter
public final class SummonSpawner {

    private final CombatantSheet caster;
    private Integer remainingRounds;
    private final Supplier<? extends CombatantSheet> spawn;
    /** Each creature's own Duração. */
    private final Integer summonRounds;

    SummonSpawner(final CombatantSheet caster, final Integer remainingRounds,
                  final Supplier<? extends CombatantSheet> spawn, final Integer summonRounds) {
        this.caster = caster;
        this.remainingRounds = remainingRounds;
        this.spawn = spawn;
        this.summonRounds = summonRounds;
    }

    /** One Rodada passes; whether it is now spent. */
    boolean tick() {
        if (remainingRounds == null) {
            return false;
        }
        remainingRounds--;
        return remainingRounds <= 0;
    }
}
