package org.aventyrs.core.sheet;

import lombok.Getter;

import java.util.UUID;

/**
 * Corpo Fechado's "se torna imune à Malefícios enquanto estiver sob efeito de Corpo Fechado" (core 0.0.94): while
 * held, {@link CombatantSheet#applyCondition} and {@link CombatantSheet#applyEffect} refuse every Malefício ({@link
 * ConditionType#isMaleficio()}). Sustained by its caster's Concentração, then 2 more Rodadas. Not cumulative.
 */
@Getter
public class MaleficioWard extends TemporaryEffect implements Sustained {

    private final UUID sustainerId;
    private Integer trailingRounds;

    public MaleficioWard(final UUID sustainerId, final int trailingRounds) {
        super((Integer) null);
        this.sustainerId = sustainerId;
        this.trailingRounds = trailingRounds;
    }

    /** Starts the trailing countdown; whether it ends at once (no trailing Rodadas). */
    boolean release() {
        if (trailingRounds == null) {
            return false;
        }
        int rounds = trailingRounds;
        trailingRounds = null;
        shortenTo(rounds);
        return rounds <= 0;
    }

    @Override
    boolean isCumulative() {
        return false;
    }
}
