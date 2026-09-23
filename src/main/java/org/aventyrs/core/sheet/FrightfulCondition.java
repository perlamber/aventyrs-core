package org.aventyrs.core.sheet;

import lombok.NonNull;

/**
 * A fear rung — Abalado, Assustado or Apavorado — cast by Frenesi Assustador, "Efeito de
 * Encantamento". A {@link Condition} so it behaves exactly like any other fear (the Desvantagem, the
 * decay down the ladder), and an {@link Enchantment} so the recipient's immunity and Ungido ward
 * reach it through {@link CombatantSheet#applyEnchantment}.
 *
 * <p>It stays itself as it decays ({@link #decayed}), because its origin matters for as long as the
 * fear lasts: "Enquanto seus PV forem menores ou iguais a zero os inimigos Abalados, Assustados e
 * Apavorados se tornam incapazes de desferir Efeitos Críticos Menores e não podem desencadear
 * Correntes de Efeitos" — read through {@link CombatantSheet#isMinorCriticalAndChainSuppressed()}.
 */
public class FrightfulCondition extends Condition implements Enchantment {

    public FrightfulCondition(@NonNull final ConditionType type, final int rounds,
                              @NonNull final CombatantSheet enchanter) {
        super(type, rounds, enchanter);
    }

    @Override
    public CombatantSheet getEnchanter() {
        return getSource();
    }

    @Override
    public boolean isHarmful() {
        return true;
    }

    @Override
    Condition decayed(final ConditionType next, final int rounds) {
        return new FrightfulCondition(next, rounds, getSource());
    }
}
