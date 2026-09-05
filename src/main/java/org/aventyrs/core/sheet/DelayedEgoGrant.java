package org.aventyrs.core.sheet;

import lombok.Getter;
import org.aventyrs.core.character.EgoDomain;

/**
 * Temporary Ego points owed to a {@link CombatantSheet} at the <em>start of its next
 * Rodada</em> — {@code GnoseAbility#ESTABILIDADE_EMOCIONAL}'s "você receberá 1 ponto
 * temporário neste Ego na Rodada seguinte". Registered by {@link
 * CombatantSheet#scheduleTemporaryEgoPointGrant}, delivered by {@link
 * CombatantSheet#startNewRound()} through {@link
 * CombatantSheet#grantTemporaryEgoPoints(EgoDomain, Object, int)}.
 *
 * <p><strong>Not a {@link TemporaryEffect}.</strong> A {@link TemporaryEffect} counts down
 * against {@link CombatantSheet#tickTemporaryEffects()}, which runs at <em>Turn end</em>
 * ({@link CombatantSheet#finishTurn()}) — so an effect registered mid-Rodada would fire its
 * first {@code applyRoundEffect} inside the very same Rodada it was registered in, which is
 * precisely the Rodada this must skip. {@link CombatantSheet#startNewRound()} is the real
 * Rodada boundary ({@code Scene#next()} calls it at the wrap), so the grant hangs off that
 * instead. The consequence, in exchange: with nothing ever calling {@code startNewRound} — no
 * live {@code Scene} and an API that doesn't mark its own Rodada boundary — the grant simply
 * waits, the same fallback {@link CombatantSheet#consumeMovementThisRound()} documents.
 *
 * <p>Distinct from {@link PendingEgoRecovery}, which owes points back at a <em>Rest</em> and
 * only ever restores what a spend took. This one hands over points that may never have been
 * spent at all, so it carries a {@code source} for the non-cumulative ceiling widening
 * {@link CombatantSheet#grantTemporaryEgoPointBonus} needs — see {@link
 * CombatantSheet#grantTemporaryEgoPoints(EgoDomain, Object, int)} for why a grant to an
 * emptied pool is two steps rather than one.
 */
@Getter
public class DelayedEgoGrant {
    private final EgoDomain domain;
    private final Object source;
    private final int value;

    public DelayedEgoGrant(final EgoDomain domain, final Object source, final int value) {
        this.domain = domain;
        this.source = source;
        this.value = value;
    }
}
