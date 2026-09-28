package org.aventyrs.core.sheet;

import lombok.Getter;
import lombok.NonNull;

/**
 * A timed state that <b>is</b> its whole effect — nothing to sum, only "is it running". {@code
 * PeritoFeat#CRIANCA_DO_MAR}'s "respirar na água … ao custo de 1PD por Rodada" and {@code
 * #REI_DA_MONTANHA}'s "grudar em paredes e tetos … ao custo de 1PD por Rodada" are the two: each
 * activation buys one Rodada of the state, and a Talento hook asks {@link
 * CombatantSheet#hasEffectFrom} whether it is still running.
 *
 * <p>Its own kind rather than a zero-valued {@code TemporaryBonus} of some {@code ModifierType}: a
 * bonus is read by whatever sums that type, and a state that grants no number must not be one.
 * Like a sourced bonus, a second activation of the same source renews it rather than stacking.
 *
 * <p><b>Counts down at the start of its holder's Turn</b>, the house reading of "por 1 Rodada"
 * (until the holder's next Turn begins): bought on one's own Turn, it covers every other
 * combatant's Turn in between, and lapses as the holder's next Turn opens — which is when the next
 * Rodada's 1PD falls due. Counting down at Turn end instead would end it the moment it was bought.
 */
@Getter
public final class SourcedState extends TemporaryEffect {

    private final String source;

    public SourcedState(@NonNull final String source, final int remainingRounds) {
        super(remainingRounds, true);
        this.source = source;
    }

    @Override
    int maximumSimultaneous() {
        return 1;
    }

    @Override
    Object stackingKey() {
        return source;
    }
}
