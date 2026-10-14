package org.aventyrs.core.sheet;

import lombok.Getter;
import lombok.NonNull;

/**
 * A Preparação held ready (core 0.1.6.17): the action named when it was taken, fired once with the extra Reação the
 * Preparação grants, until its holder's next Turn begins (table ruling, 2026-10-14 — the "por 1 Rodada" reading). The
 * trigger is narrated, never checked. Applied and fired through {@code PreparationService}.
 */
@Getter
public class PreparedAction extends TemporaryEffect {

    /** The prepared action as the player named it — "Ataque com a Lança contra quem cruzar a porta". */
    private final String description;

    /** Its own Tempo de Ação — at most {@code PreparationService#MAXIMUM_PREPARED_COST}. */
    private final ActionCost preparedCost;

    private boolean fired;

    public PreparedAction(@NonNull final String description, @NonNull final ActionCost preparedCost) {
        super(1, true);
        this.description = description;
        this.preparedCost = preparedCost;
    }

    /** Spends it — the extra Reação is used. */
    void fire() {
        this.fired = true;
    }

    @Override
    boolean isCumulative() {
        return false;
    }

    @Override
    public boolean isExpired() {
        return fired || super.isExpired();
    }
}
