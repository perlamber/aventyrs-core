package org.aventyrs.core.effect;

import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.skill.CriticalResult;

import static org.aventyrs.core.util.TranslatableMessages.CRITICAL_EFFECT_REQUIRES_A_CRITICAL_HIT;

/**
 * An Efeito Crítico — a secondary effect specifically triggered by a critical roll
 * outcome (see {@link CriticalResult}), e.g. {@code
 * org.aventyrs.core.skill.ataquecorpoacorpo.AtaqueCorpoACorpoCompetencyAbility
 * #ABRIR_DEFESAS}'s Malefício Desprevenido on a critical hit. Still just a marker on top
 * of {@link Effect} beyond {@link #validateCriticalHit} below — nothing here should be
 * inferred beyond "this is an Effect that is an Efeito Crítico": no critical-detection
 * wiring, no Malefício/status-effect tracking, no duration/Rodada tracking beyond what a
 * concrete implementation builds for itself (see {@code Sangramento}/{@code ManaPurge}).
 * See {@code org.aventyrs.core.effect} package-info for the pipeline this fits into.
 */
public interface CriticalEffect extends Effect {

    /**
     * Every concrete CriticalEffect built so far (e.g. {@code Sangramento}, {@code
     * ManaPurge}) is only ever a *consequence* of a critical hit landing, never something
     * a caller applies on its own judgment — each rejects any {@code criticalResult}
     * that isn't {@link CriticalResult#ACERTO_CRITICO_MAIOR}/{@link
     * CriticalResult#ACERTO_CRITICO_MENOR} at construction. Shared here once two
     * concrete implementations needed the identical check, the same "validate
     * possession/legitimacy up front rather than compute a result for an illegitimate
     * case" discipline {@code AbstractSkillInteraction#validateRequestedTrait} already
     * applies to a requested Habilidade/Especialização.
     * @throws IllegalOperationException if criticalResult isn't an Acerto Crítico
     */
    static void validateCriticalHit(final CriticalResult criticalResult) {
        if (criticalResult != CriticalResult.ACERTO_CRITICO_MAIOR && criticalResult != CriticalResult.ACERTO_CRITICO_MENOR) {
            throw new IllegalOperationException(CRITICAL_EFFECT_REQUIRES_A_CRITICAL_HIT);
        }
    }
}
