package org.aventyrs.core.sheet;

import lombok.Getter;
import lombok.NonNull;
import org.aventyrs.core.magic.ElementalType;

/**
 * Resistência Elemental held for a Duração — {@code instances} of RE against {@code element}, summed by {@link
 * CombatantSheet#getElementalResistanceInstances}. Autocontrole's "receber RA por 1 Rodada; adicionalmente, durante
 * este tempo, receber RD, RM ou RE, à sua escolha" (core 0.0.81) is the first grant: RE has no {@code ModifierType}
 * (it is per element), so it needs an effect of its own where RD/RM ride a {@link TemporaryBonus}.
 */
@Getter
public class TimedElementalResistance extends TemporaryEffect {
    private final ElementalType element;
    private final int instances;

    /** countsDownAtTurnStart — see {@link TemporaryEffect#countsDownAtTurnStart()}. */
    public TimedElementalResistance(@NonNull final ElementalType element, final int instances, final int rounds,
                                    final boolean countsDownAtTurnStart) {
        super(rounds, countsDownAtTurnStart);
        this.element = element;
        this.instances = instances;
    }
}
