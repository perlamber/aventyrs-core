package org.aventyrs.core.sheet;

import lombok.Getter;

/**
 * Procrastinar Ferimento (core 0.0.94): "os PV que você, ou um aliado em Distância Curta, perderia em decorrência de um
 * ataque sejam perdidos apenas na Rodada seguinte". Cast as a Reação before the attack resolves — the {@code
 * GUARDA_VIDAS} ordering — it waits on its holder for the hit and is spent by the first attack damage that lands ({@code
 * DamageInteraction} postpones it), or lapses with the Rodada (⚠️ 1 Rodada).
 *
 * <p>Estancar ({@link #isStanched()}): "Se o dano sofrido fosse causar efeitos de Sangramento ele não causará" — the
 * hit it postpones leaves a {@link StanchedWound} behind, which refuses a {@link Bleeding} for that Rodada.
 */
@Getter
public class PostponedWoundWard extends TemporaryEffect {

    /** It waits for the hit for this long. */
    public static final int ROUNDS = 1;

    private boolean stanched;

    public PostponedWoundWard() {
        super(ROUNDS);
    }

    /** Its Corrente Estancar fired. */
    public void stanch() {
        this.stanched = true;
    }

    @Override
    boolean isCumulative() {
        return false;
    }
}
