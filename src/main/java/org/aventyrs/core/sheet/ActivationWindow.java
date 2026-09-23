package org.aventyrs.core.sheet;

import lombok.Getter;
import lombok.NonNull;

/**
 * A trait's "Nesta Rodada …" / "Por 1 Rodada …" state, when what the trait changes is not a stat
 * a {@link Blessing} could carry but a question other clauses ask — {@code
 * SenhorDaBrigaAbility#FINALIZACAO}'s "ataques bem-sucedidos com Armas Naturais recebem a Corrente
 * de Efeitos – Finalização", or {@code FantasmaDoRingueAbility#CRUZ_DE_SANGUE}'s widened Margem
 * Crítica, which has no {@code ModifierType} at all.
 *
 * <p>Carries nothing but its source and its Duração: the clause that reads it knows what it means.
 * Opened through {@link CombatantSheet#openActivationWindow} and asked about through {@link
 * CombatantSheet#hasActivationWindow}. One per source — re-opening renews it.
 */
@Getter
public class ActivationWindow extends TemporaryEffect {

    private final Object source;

    public ActivationWindow(@NonNull final Object source, final int rounds) {
        this(source, rounds, false);
    }

    /** With countsDownAtTurnStart, closes as its holder's Turn begins rather than as it ends. */
    public ActivationWindow(@NonNull final Object source, final int rounds, final boolean countsDownAtTurnStart) {
        super(rounds, countsDownAtTurnStart);
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
