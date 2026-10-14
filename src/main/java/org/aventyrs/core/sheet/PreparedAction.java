package org.aventyrs.core.sheet;

import lombok.Getter;
import lombok.NonNull;

/**
 * A Preparação held ready (core 0.1.6.17): the action picked when it was taken, fired once with the extra Reação the
 * Preparação grants, until its holder's next Turn begins (table ruling, 2026-10-14 — the "por 1 Rodada" reading). The
 * trigger is narrated, never checked. Applied and fired through {@code PreparationService}.
 *
 * <p>Since core 0.1.6.21 it holds <b>which</b> action — a {@link PreparedActionKind} and a caller-chosen key — not a
 * description: its targets and options are chosen, and its PM/PD/PV paid, when it is fired (table ruling, 2026-10-14).
 */
@Getter
public class PreparedAction extends TemporaryEffect {

    /** Which of the caller's paths runs it. */
    private final PreparedActionKind kind;

    /**
     * The action within its kind, as the caller resolves it back when firing — a Título and trait name, a Magia's
     * catalog key, an attack option, a competency ability. Opaque to core.
     */
    private final String key;

    /** The action's display name — "Bola de Fogo", "Ataque com a Lança". */
    private final String label;

    /** Its own Tempo de Ação — at most {@code PreparationService#MAXIMUM_PREPARED_COST}. */
    private final ActionCost preparedCost;

    private boolean fired;

    public PreparedAction(@NonNull final PreparedActionKind kind, @NonNull final String key,
                          @NonNull final String label, @NonNull final ActionCost preparedCost) {
        super(1, true);
        this.kind = kind;
        this.key = key;
        this.label = label;
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
