package org.aventyrs.core.sheet;

import lombok.Getter;
import lombok.NonNull;
import org.aventyrs.core.character.DamageDescriptor;

/**
 * A stated, typed damage every Rodada — "Alvo sofre 3 pontos de Dano Mágico Elemental por 2+Vigor do Alvo Rodadas"
 * (Marca Elemental, core 0.1.7.3). The flat twin of {@link RecurringDice}: no die, so it acts on its own, mitigated
 * through {@code DamageService} like any hit of its {@link DamageDescriptor}.
 *
 * <p>One per source at a time ("não cumulativo"): a second landing replaces the first, renewing the Duração.
 */
@Getter
public class RecurringDamage extends TemporaryEffect {

    private final int valuePerRound;

    @NonNull
    private final DamageDescriptor descriptor;

    @NonNull
    private final String source;

    public RecurringDamage(final int valuePerRound, @NonNull final DamageDescriptor descriptor, final int rounds,
                           @NonNull final String source) {
        super(rounds);
        this.valuePerRound = valuePerRound;
        this.descriptor = descriptor;
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

    @Override
    void applyRoundEffect(final CombatantSheet sheet) {
        new org.aventyrs.core.character.services.DamageServiceImpl()
                .applyDamage(sheet, null, descriptor, null, valuePerRound, false);
    }

    @Override
    public ContinuousNature getContinuousNature() {
        return ContinuousNature.damaging(descriptor);
    }
}
