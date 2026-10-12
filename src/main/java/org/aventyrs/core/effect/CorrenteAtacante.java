package org.aventyrs.core.effect;

import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.ResourceType;
import org.aventyrs.core.util.DiceRoller;

/**
 * Corrente Atacante — the Corrente com Peso Aprimoramento's Corrente: "Dano +1d6."
 *
 * <p>The die is thrown with the attack's own {@link DiceRoller} and lands as this stage runs — ⚠️ after the head's
 * mitigation, like {@link Rugido}'s +2 and {@link Incendiar}'s +1d6, so RD does not reach it. With no roller it is
 * reported unrolled on {@link InteractionResult#getExtraDamageDice()} instead.
 */
public class CorrenteAtacante extends AbstractEffect implements EffectChain {

    /** "Dano +1d6". */
    public static final int EXTRA_DICE = 1;

    private final DiceRoller roller;

    /** @param roller the attack's own dice roller, or {@code null} to report the die rather than throw it */
    public CorrenteAtacante(final DiceRoller roller) {
        this.roller = roller;
    }

    @Override
    public String getDescription() {
        return "Corrente Atacante: Dano +1d6.";
    }

    @Override
    public InteractionResult applyTo(final CombatantSheet target) {
        InteractionResult.InteractionResultBuilder result = InteractionResult.builder();
        if (roller == null) {
            result.extraDamageDice(EXTRA_DICE);
        } else {
            int before = target.getDamageTaken();
            target.applyDamage(roller.rollD6(EXTRA_DICE));
            result.resourceLossValue(target.getDamageTaken() - before).resourceLossType(ResourceType.HIT_POINTS);
        }
        return reportChain(result.resultStatus(resolveStatus(target))).build();
    }
}
