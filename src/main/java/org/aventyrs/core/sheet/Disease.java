package org.aventyrs.core.sheet;

import lombok.Getter;

import java.util.List;

/**
 * A {@link ConditionType#DOENTE} that knows whether it spreads — "Situacionalmente, conforme descrição,
 * pode se propagar para personagens adjacentes ou em contato." Every other magnitude a Doença states
 * rides {@link #getExtraEffects()}; its {@link #getSource()} is the creature that infected, which is
 * immune to its own disease ("Monstros capazes de adoecer seus alvos … são imunes a própria doença").
 * See {@code DiseaseService} for the spreading itself.
 */
@Getter
public class Disease extends Condition {

    private final boolean propagates;

    public Disease(final Integer remainingRounds, final CombatantSheet source, final boolean propagates,
                   final List<ConditionType.ConditionEffect> extraEffects) {
        super(ConditionType.DOENTE, remainingRounds, source, extraEffects);
        this.propagates = propagates;
    }

    /** This same disease caught by someone else — same source, same remaining Duração. */
    public Disease caught() {
        return new Disease(getRemainingRounds(), getSource(), propagates, getExtraEffects());
    }

    @Override
    Condition decayed(final ConditionType next, final int rounds) {
        return next == ConditionType.DOENTE
                ? new Disease(rounds, getSource(), propagates, getExtraEffects())
                : super.decayed(next, rounds);
    }
}
