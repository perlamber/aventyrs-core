package org.aventyrs.core.sheet;

/**
 * Imunizar — "alvo se torna imune a encantamentos nocivos e maldições" for its Duração. While held,
 * {@link CombatantSheet#applyEnchantment} refuses a harmful {@link Enchantment} and {@link
 * CombatantSheet#applyCondition} refuses {@link ConditionType#AMALDICOADO}, the one Maldição this
 * core names as a Condição. Not cumulative: a second ward renews the first.
 */
public class EnchantmentWard extends TemporaryEffect {

    public EnchantmentWard(final int rounds) {
        super(rounds);
    }

    private EnchantmentWard() {
        super((Integer) null);
    }

    /** A ward with no Duração — held for as long as its holder stands (the Anciente's immunity). */
    public static EnchantmentWard openEnded() {
        return new EnchantmentWard();
    }

    @Override
    boolean isCumulative() {
        return false;
    }
}
