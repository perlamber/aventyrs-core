package org.aventyrs.core.sheet;

import lombok.Getter;
import lombok.NonNull;

/**
 * {@link ConditionType#DEVORADO} held with what Bocarra's Devorar Inteiro needs to know about it:
 * who swallowed this character ({@link #getSource()}, never {@code null} here), how much of the
 * captor's stomach they fill, and how much damage they have dealt from inside — "para se soltarem
 * precisam causar danos (dobro do vigor do Ogro) até serem regurgitados".
 *
 * <p>Open-ended: a swallowed character stays inside until they break out, are regurgitated, or are
 * digested. Applied and lifted only by {@code DevourService}, which also keeps the captor's own
 * list of victims ({@link CombatantSheet#getDevouredVictims()}) in step.
 */
@Getter
public class Devoured extends Condition {

    private final int vigorOccupied;
    private int damageFromInside;

    public Devoured(@NonNull final CombatantSheet captor, final int vigorOccupied) {
        super(ConditionType.DEVORADO, null, captor);
        this.vigorOccupied = vigorOccupied;
    }

    /** Adds damage dealt to the captor from inside; returns the running total. */
    public int recordDamageFromInside(final int amount) {
        damageFromInside += Math.max(0, amount);
        return damageFromInside;
    }
}
