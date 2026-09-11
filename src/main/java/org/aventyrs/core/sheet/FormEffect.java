package org.aventyrs.core.sheet;

import lombok.Getter;

/**
 * The countdown behind a Forma entered with a stated Duração — "A Duração na forma de Draconato é
 * de 3 Rodadas". Holds no bonus of its own: the Forma itself lives on {@link
 * CombatantSheet#getCurrentForm()}, and this exists only so that something expires and puts its
 * holder back into their own shape.
 *
 * <p>Applied by {@code ActiveAbilityService#activate} for an ability whose {@code
 * ActiveAbility#resolveGrantedForm} names one, and consumed by {@link
 * CombatantSheet#tickTemporaryEffects()}, which returns the sheet to its own shape as this
 * expires — the same "act as it lapses" special case a decaying {@link Condition} already gets,
 * and for the same reason: an expiring effect that must *do* something cannot be swept out
 * silently.
 *
 * <p><b>{@code remainingRounds} may be {@code null}</b>, for a Forma its holder stays in until
 * they leave it — see {@code ActiveAbility#resolveDurationInRounds()}. Such an effect never
 * expires from ticking, so nothing ever puts the sheet back: {@code
 * CombatantSheet#enterForm(null)} is the only way out, which is exactly what a toggle means.
 *
 * <p><b>Not cumulative.</b> A combatant is in one shape at a time, so a second Forma must replace
 * the first rather than leaving two countdowns racing to un-transform the same sheet.
 */
@Getter
public class FormEffect extends TemporaryEffect {

    private final FormType form;

    public FormEffect(final FormType form, final Integer remainingRounds) {
        super(remainingRounds);
        this.form = form;
    }

    @Override
    boolean isCumulative() {
        return false;
    }
}
