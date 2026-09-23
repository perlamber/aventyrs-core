package org.aventyrs.core.sheet;

import lombok.Getter;

import java.util.function.Predicate;

/**
 * Temporary Ego points owed to a {@link CombatantSheet} at the start of a <em>later</em> Rodada —
 * {@code GnoseAbility#ESTABILIDADE_EMOCIONAL}'s "você receberá 1 ponto temporário neste Ego na
 * Rodada seguinte", and Uno com a Ira's "são recuperados após 2 Rodadas, mas apenas se seu Frenesi
 * ainda estiver ativo e você estiver consciente". Registered by {@link
 * CombatantSheet#scheduleTemporaryEgoPointGrant}, delivered by {@link CombatantSheet#startNewRound()}
 * once its Rodadas have passed.
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
 * <p>Two kinds. A <b>grant</b> hands over points that may never have been spent, so it widens the
 * ceiling for its {@code source} first (see {@link CombatantSheet#grantTemporaryEgoPoints(
 * org.aventyrs.core.character.EgoDomain, Object, int)}). A <b>{@linkplain #isRecovery() recovery}</b>
 * only gives back points that were spent — Uno com a Ira's "recuperados" — and also clears that many
 * from what the holder is still owed by the hour ({@link CombatantSheet#passHours}).
 *
 * <p>A {@link #getGuard() guard} is checked at delivery: a grant whose condition no longer holds
 * ("se seu Frenesi ainda estiver ativo") is dropped rather than delivered.
 *
 * <p>Distinct from {@link PendingEgoRecovery}, which owes points back at a <em>Rest</em>.
 */
@Getter
public class DelayedEgoGrant {
    private final org.aventyrs.core.character.EgoDomain domain;
    private final Object source;
    private final int value;
    private int remainingRounds;
    private final Predicate<CombatantSheet> guard;
    private final boolean recovery;

    public DelayedEgoGrant(final org.aventyrs.core.character.EgoDomain domain, final Object source, final int value) {
        this(domain, source, value, 1, sheet -> true, false);
    }

    public DelayedEgoGrant(final org.aventyrs.core.character.EgoDomain domain, final Object source, final int value,
                           final int rounds, final Predicate<CombatantSheet> guard, final boolean recovery) {
        if (rounds < 1) {
            throw new IllegalArgumentException("A delayed Ego grant waits at least one Rodada: " + rounds);
        }
        this.domain = domain;
        this.source = source;
        this.value = value;
        this.remainingRounds = rounds;
        this.guard = guard;
        this.recovery = recovery;
    }

    /** One Rodada boundary passed; true once it is due. */
    boolean advance() {
        remainingRounds--;
        return remainingRounds <= 0;
    }
}
