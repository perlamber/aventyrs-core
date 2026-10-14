package org.aventyrs.core.effect;

import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.ResourceType;

/** The "+N to this Magia's damage or healing" half every artefact Corrente shares: damage an enemy, heal an ally. */
final class SpellEffectBonus {

    private SpellEffectBonus() {
    }

    static InteractionResult.InteractionResultBuilder apply(final CombatantSheet target, final boolean hostile,
                                                            final int amount) {
        int before = target.getDamageTaken();
        if (hostile) {
            target.applyDamage(amount);
            return InteractionResult.builder()
                    .resourceLossValue(target.getDamageTaken() - before)
                    .resourceLossType(ResourceType.HIT_POINTS);
        }
        target.heal(amount);
        return InteractionResult.builder();
    }
}
