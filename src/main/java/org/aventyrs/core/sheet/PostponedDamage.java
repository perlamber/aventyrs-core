package org.aventyrs.core.sheet;

import lombok.Getter;

/**
 * Pontos de Vida a {@link CombatantSheet} will lose at the <em>start of its next Rodada</em>
 * rather than now — {@code VidaSpell#ALIVIAR_A_DOR}'s Efeito Alternativo <i>Procrastinar
 * Ferimento</i>, "os PV que você, ou um aliado em Distância Curta, perderia em decorrência de um
 * ataque sejam perdidos apenas no Rodada seguinte". Registered by {@link
 * CombatantSheet#schedulePostponedDamage}, delivered by {@link CombatantSheet#startNewRound()}.
 *
 * <p>The damage is <b>deferred, not cancelled</b>. The hit landed, its magnitude is already
 * mitigated, and every consequence other than the PV loss happens on schedule — {@code
 * DamageInteraction} still reports the figure as {@code resourceLossValue} and still forwards its
 * chain, so the Correntes and Efeitos Críticos that hit triggered are unaffected.
 *
 * <p><strong>Not a {@link TemporaryEffect}</strong>, for exactly the reason {@link
 * DelayedEgoGrant} is not: a {@link TemporaryEffect} ticks against {@link
 * CombatantSheet#tickTemporaryEffects()}, which runs at <em>Turn end</em> ({@link
 * CombatantSheet#finishTurn()}), so damage registered mid-Rodada would land inside the very Rodada
 * it must skip. {@link CombatantSheet#startNewRound()} is the real Rodada boundary ({@code
 * Scene#next()} calls it at the wrap). With no live {@code Scene} ever calling it the damage
 * simply waits, the same fallback {@link CombatantSheet#consumeMovementThisRound()} documents.
 *
 * <p>It lands through {@link CombatantSheet#applyDamage(int)} directly — the mitigation already
 * ran when the hit was resolved, and re-running it a Rodada later would apply the target's RD
 * twice. That also means it triggers no damage-taken reaction: {@code
 * DamageService#notifyDamageTaken} fired at resolution, which is when the attack was actually
 * suffered.
 *
 * <p>{@code source} is who dealt it, kept for the same reason {@link DelayedEgoGrant} keeps one —
 * so a consumer can tell postponed hits apart. Nothing reads it yet.
 */
@Getter
public class PostponedDamage {

    private final int amount;
    private final CombatantSheet source;

    public PostponedDamage(final int amount, final CombatantSheet source) {
        this.amount = amount;
        this.source = source;
    }
}
