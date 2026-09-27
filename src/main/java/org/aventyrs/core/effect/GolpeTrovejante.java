package org.aventyrs.core.effect;

import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.InteractionResult;

/**
 * Golpe Trovejante — "Se este ataque for um Acerto Crítico, o Efeito Crítico natural de sua arma é
 * aplicado duas vezes." A Corrente de Efeitos granted by {@code DuelistaFeat#MAESTRIA_EM_ARMA}.
 *
 * <p>Unlike every other {@link EffectChain}, it does nothing to its target itself: what it changes
 * is <b>how many times the weapon's own Efeito Crítico is built</b>, and that is decided where the
 * chain is assembled. {@code org.aventyrs.core.combat.AttackDelivery} counts the Golpes Trovejantes
 * a triggered Corrente carries ({@link #extraNaturalCriticalEffectApplications}) and hands them to
 * the critical-effect resolution as extra applications of the natural effect — the same knob
 * Finalização turns. On a hit that is not a critical it therefore changes nothing, exactly as its
 * text says. Its {@link #applyTo} only passes the chain on.
 */
public class GolpeTrovejante extends AbstractEffect implements EffectChain {

    /** "aplicado duas vezes" — once more than the critical already applies it. */
    private static final int EXTRA_APPLICATIONS = 1;

    @Override
    public String getDescription() {
        return "Se este ataque for um Acerto Crítico, o Efeito Crítico natural de sua arma é aplicado duas vezes.";
    }

    /** How many more times the natural Efeito Crítico is applied on a critical this Corrente rides. */
    public int extraNaturalCriticalEffectApplications() {
        return EXTRA_APPLICATIONS;
    }

    @Override
    public InteractionResult applyTo(final CombatantSheet target) {
        return reportChain(InteractionResult.builder().resultStatus(resolveStatus(target))).build();
    }
}
