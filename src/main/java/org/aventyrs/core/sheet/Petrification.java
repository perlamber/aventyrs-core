package org.aventyrs.core.sheet;

import lombok.NonNull;

/**
 * {@link ConditionType#PETRIFICADO} as the Encantamento it is — Olhar de Lacerto's Olhar
 * Petrificador. Applied through {@link CombatantSheet#applyEnchantment}, so a target immune to
 * Encantamentos is never petrified and a warded one is petrified for half as long. A {@code null}
 * Duração is "petrificado permanentemente".
 */
public class Petrification extends Condition implements Enchantment {

    public Petrification(@NonNull final CombatantSheet enchanter, final Integer rounds) {
        super(ConditionType.PETRIFICADO, rounds, enchanter);
    }

    @Override
    public CombatantSheet getEnchanter() {
        return getSource();
    }

    @Override
    public boolean isHarmful() {
        return true;
    }
}
