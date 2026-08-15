package org.aventyrs.core.sheet;

import lombok.Getter;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.rest.RestType;

/**
 * Temporary Ego points owed back to a CharacterSheet at its next qualifying Rest — e.g.
 * {@code org.aventyrs.core.effect.Primor}'s own "pontos perdidos desta forma são
 * recuperados no próximo Descanso[ Longo] do alvo" clause: unlike an ordinary temporary
 * Ego point spend (which has no automatic restoration), Primor's own rules text
 * specifically promises these points back once the target rests. Registered via {@link
 * CharacterSheet#owePendingEgoRecovery}, resolved by {@link
 * CharacterSheet#applyPendingEgoRecoveries} — called by {@code RestServiceImpl#applyRest}
 * whenever a Rest is actually taken, since (unlike Scene's still-nonexistent turn
 * shifter) {@code RestService} already is the real, complete "a Rest happened" trigger
 * point.
 *
 * <p>{@code minimumRestType} is compared by {@link RestType}'s own ordinal order
 * (MINIMO &lt; CURTO &lt; LONGO &lt; TOTAL) — a Rest satisfies this recovery once its tier
 * is at least {@code minimumRestType}'s. Primor Menor's unqualified "no próximo Descanso"
 * is read as satisfied by *any* Rest tier (minimum {@link RestType#MINIMO}); Primor
 * Maior's "no próximo Descanso Longo" specifically requires at least {@link
 * RestType#LONGO}. This is an inference from the ordered RestType hierarchy, not
 * confirmed against a more granular rules-text definition of unqualified "Descanso" —
 * flagged the same way {@code CriticalResult}'s own UNIMAGINABLE inference is.
 */
@Getter
public class PendingEgoRecovery {
    private final EgoDomain domain;
    private final int value;
    private final RestType minimumRestType;

    public PendingEgoRecovery(final EgoDomain domain, final int value, final RestType minimumRestType) {
        this.domain = domain;
        this.value = value;
        this.minimumRestType = minimumRestType;
    }

    /** True once a Rest of at least {@link #minimumRestType}'s tier has been taken. */
    boolean isSatisfiedBy(final RestType restType) {
        return restType.ordinal() >= minimumRestType.ordinal();
    }
}
