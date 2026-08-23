package org.aventyrs.core.sheet;

import lombok.Getter;

import java.util.Optional;

/**
 * Roubo de Vida — an ongoing buff letting its holder recover PV by dealing damage, the
 * opposite of {@link Bleeding} (which drains a target's own PV without transferring it to
 * anyone). Registered via {@link CombatantSheet#applyEffect(TemporaryEffect)}, and counts
 * down in Rodadas via its shared superclass {@link TemporaryEffect} the same way {@link
 * Bleeding}/{@link ManaDrain}/{@link Withering} do.
 *
 * <p>Unlike those three, {@code applyRoundEffect} is left as {@link TemporaryEffect}'s own
 * no-op default — Roubo de Vida doesn't do anything automatically each Rodada; {@code value}
 * only matters the moment its holder actually deals damage, which this library never resolves
 * (see the {@code skill} package-info's "What this library computes" section) — a caller
 * resolving a dealt hit reads the total via {@link
 * org.aventyrs.core.character.services.LifeStealService#getTotalLifeSteal} and applies the
 * healing itself, via {@link CombatantSheet#heal(int)}.
 *
 * <p>Multiple instances can stack ({@link #isCumulative()}'s own default) — nothing about
 * Roubo de Vida itself says otherwise; {@code VigorAbility#METABOLISMO_RAPIDO}'s own "não
 * cumulativo" clause is about its own flat +1 bonus never repeating regardless of how many
 * LifeSteal effects are active, not about the effects themselves (see {@link
 * org.aventyrs.core.character.services.LifeStealServiceImpl} for where that's actually
 * enforced).
 */
@Getter
public class LifeSteal extends TemporaryEffect {
    private final int value;

    public LifeSteal(final int value, final Optional<Integer> remainingRounds) {
        super(remainingRounds.orElse(null));
        this.value = value;
    }
}
