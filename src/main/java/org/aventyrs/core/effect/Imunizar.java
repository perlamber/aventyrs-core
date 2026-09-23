package org.aventyrs.core.effect;

import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.EnchantmentWard;
import org.aventyrs.core.sheet.InteractionResult;

/**
 * Imunizar — "alvo se torna imune a encantamentos nocivos e maldições" for 5 Rodadas (Maior) / 2
 * (Menor): a {@code sheet.EnchantmentWard}, which {@code CombatantSheet#applyEnchantment} and {@code
 * applyCondition} (for Amaldiçoado) consult. A Magia's beneficial critical.
 */
public class Imunizar extends AbstractCriticalEffect {


    public Imunizar(final CriticalEffectContext context) {
        super(context);
    }

    @Override
    public CriticalEffectType getType() {
        return CriticalEffectType.IMUNIZAR;
    }

    @Override
    protected String majorDescription() {
        return "alvo se torna imune a encantamentos nocivos e maldições por 5 Rodadas.";
    }

    @Override
    protected String minorDescription() {
        return "alvo se torna imune a encantamentos nocivos e maldições por 2 Rodadas.";
    }

    @Override
    protected InteractionResult.InteractionResultBuilder resolve(final CombatantSheet target) {
        target.applyEffect(new EnchantmentWard(pick(5, 2)));
        return InteractionResult.builder();
    }
}
