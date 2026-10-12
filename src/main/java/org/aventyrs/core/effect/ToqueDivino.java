package org.aventyrs.core.effect;

import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.InteractionResult;

/**
 * Toque Divino — the Báculo's Corrente on the Magias its wielder casts: "Efeitos de danos e curas desta sua magia
 * aumentam em +2."
 *
 * <p>The +2 goes to whichever half the Magia did to this target: damage to an enemy, healing to an ally. ⚠️ It lands
 * as this stage runs, after the Magia's own mitigation, like {@link Rugido}'s +2.
 */
public class ToqueDivino extends AbstractEffect implements EffectChain {

    /** "aumentam em +2". */
    public static final int BONUS = 2;

    private final boolean hostile;

    /** @param hostile whether the Magia landed on an enemy (damage) rather than an ally (healing) */
    public ToqueDivino(final boolean hostile) {
        this.hostile = hostile;
    }

    @Override
    public String getDescription() {
        return "Efeitos de danos e curas desta sua magia aumentam em +2.";
    }

    @Override
    public InteractionResult applyTo(final CombatantSheet target) {
        return reportChain(SpellEffectBonus.apply(target, hostile, BONUS).resultStatus(resolveStatus(target))).build();
    }
}
