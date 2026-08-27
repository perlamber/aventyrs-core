package org.aventyrs.core.effect;

import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.rest.RestType;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.EgoPointSpend;
import org.aventyrs.core.sheet.EgoPointType;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.PendingEgoRecovery;
import org.aventyrs.core.skill.CriticalResult;

import static org.aventyrs.core.util.TranslatableMessages.INVALID_PRIMOR_EGO_DOMAIN;

/**
 * Primor — the third concrete {@link CriticalEffect} (see {@code
 * org.aventyrs.core.effect} package-info for the pipeline this fits into), and the first
 * that isn't a PV/PM/PD resource drain: it costs the character receiving the attack —
 * {@link #applyTo}'s {@code target}, never the attacker — temporary Ego points from
 * either Sorte or Autocontrole (never Recursos/Iniciativa; rejected at construction, see
 * below), drawn from that domain's <em>temporary</em> pool via {@link
 * CombatantSheet#spendEgoPoints} — never the permanent one: nothing in this ruleset reduces
 * permanent Ego points automatically, they are only ever spent by their holder's own choice.
 * The spend floors at 0 rather than throwing, and the {@link PendingEgoRecovery} below is
 * registered for what was <em>actually</em> spent rather than what was asked for, so a target
 * holding fewer temporary points than this drains is never handed back points it had spent
 * itself. Unlike
 * {@link Sangramento}/{@link ManaPurge}, there's no ongoing per-Rodada loss — this is a
 * one-time spend — but the spent points are specifically promised back at the target's
 * next qualifying Rest ("pontos perdidos desta forma são recuperados no próximo
 * Descanso[ Longo] do alvo"), registered as a {@link PendingEgoRecovery} (see {@link
 * CombatantSheet#owePendingEgoRecovery}) and resolved for real by {@code
 * RestServiceImpl#applyRest} — {@code RestService} already is a complete, real "a Rest
 * happened" trigger, unlike Scene's still-nonexistent turn shifter, so this doesn't need
 * to be left as an unconnected scaffold.
 *
 * <p>Which Ego domain (Sorte or Autocontrole) is drained is "definido aleatoriamente" per
 * its own rules text — this core never rolls dice or makes random choices itself (see
 * {@code org.aventyrs.core.skill} package-info's "What this library computes" section),
 * so the caller supplies the already-randomly-resolved {@link EgoDomain} at construction,
 * the same way {@link Sangramento}/{@link ManaPurge} are handed an already-resolved
 * {@link CriticalResult} rather than rolling their own dice.
 *
 * <p>Its severity is determined directly by the {@link CriticalResult} that triggered
 * it, not picked by hand — {@link CriticalResult#ACERTO_CRITICO_MAIOR} for Primor Maior
 * (2 points, requires at least a {@link RestType#LONGO} to recover), {@link
 * CriticalResult#ACERTO_CRITICO_MENOR} for Primor Menor (1 point, any Rest tier recovers
 * it — see {@link PendingEgoRecovery}'s own javadoc for why unqualified "Descanso" is
 * read as {@link RestType#MINIMO}, an inference rather than confirmed rules text). Any
 * other {@code CriticalResult} — a plain roll, or either Falha Crítica — is rejected at
 * construction ({@link IllegalOperationException}, via {@link
 * CriticalEffect#validateCriticalHit}): Primor is only ever a *consequence* of a critical
 * hit landing, never something a caller applies on its own judgment.
 */
public class Primor extends AbstractEffect implements CriticalEffect {

    private static final int MAIOR_VALUE = 2;
    private static final int MENOR_VALUE = 1;

    private final CriticalResult criticalResult;
    private final EgoDomain domain;

    public Primor(final CriticalResult criticalResult, final EgoDomain domain) {
        CriticalEffect.validateCriticalHit(criticalResult);
        if (domain != EgoDomain.SORTE && domain != EgoDomain.AUTOCONTROLE) {
            throw new IllegalOperationException(INVALID_PRIMOR_EGO_DOMAIN);
        }
        this.criticalResult = criticalResult;
        this.domain = domain;
    }

    @Override
    public CriticalEffectType getType() {
        return CriticalEffectType.PRIMOR;
    }

    @Override
    public String getDescription() {
        return criticalResult == CriticalResult.ACERTO_CRITICO_MAIOR
                ? "Alvo perde 2 pontos temporários de Sorte ou Autocontrole (definido " +
                        "aleatoriamente), pontos perdidos desta forma são recuperados no " +
                        "próximo Descanso Longo do alvo."
                : "Alvo perde 1 ponto temporário de Sorte ou Autocontrole (definido " +
                        "aleatoriamente), pontos perdidos desta forma são recuperados no " +
                        "próximo Descanso do alvo.";
    }

    @Override
    public InteractionResult applyTo(final CombatantSheet target) {
        int value = criticalResult == CriticalResult.ACERTO_CRITICO_MAIOR ? MAIOR_VALUE : MENOR_VALUE;
        RestType minimumRestType = criticalResult == CriticalResult.ACERTO_CRITICO_MAIOR ? RestType.LONGO : RestType.MINIMO;

        EgoPointSpend spend = target.spendEgoPoints(domain, EgoPointType.TEMPORARY, value);
        target.owePendingEgoRecovery(new PendingEgoRecovery(domain, spend.getValue(), minimumRestType));

        return reportChain(InteractionResult.builder()
                .resultStatus(resolveStatus(target))
                .egoLossValue(spend.getValue())
                .egoLossDomain(domain))
                .build();
    }
}
