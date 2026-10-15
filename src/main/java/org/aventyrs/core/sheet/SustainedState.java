package org.aventyrs.core.sheet;

import lombok.Getter;
import lombok.NonNull;

import java.util.UUID;

/**
 * A {@link SourcedState} whose Duração is "Concentração + N Rodadas" (core 0.1.7.4) — Aura do Paradoxo's "A Duração
 * da Aura do Paradoxo é de Concentração + 2 Rodadas". No countdown while its holder concentrates; once the focus is
 * lost ({@link CombatantSheet#loseConcentration()}), {@link #release()} starts the trailing N, exactly as a {@link
 * MaleficioWard}. {@link CombatantSheet#hasEffectFrom} answers for it by source, and one per source runs at a time.
 */
@Getter
public final class SustainedState extends TemporaryEffect implements Sustained {

    private final String source;
    private final UUID sustainerId;
    private Integer trailingRounds;

    public SustainedState(@NonNull final String source, @NonNull final UUID sustainerId, final int trailingRounds) {
        super((Integer) null);
        this.source = source;
        this.sustainerId = sustainerId;
        this.trailingRounds = trailingRounds;
    }

    @Override
    public boolean release() {
        if (trailingRounds == null) {
            return false;
        }
        int rounds = trailingRounds;
        trailingRounds = null;
        startCountdown(rounds);
        return rounds <= 0;
    }

    @Override
    int maximumSimultaneous() {
        return 1;
    }

    @Override
    Object stackingKey() {
        return source;
    }
}
