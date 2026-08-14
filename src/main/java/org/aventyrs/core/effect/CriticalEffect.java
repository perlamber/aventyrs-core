package org.aventyrs.core.effect;

/**
 * An Efeito Crítico — a secondary effect specifically triggered by a critical roll
 * outcome (see {@code org.aventyrs.core.skill.CriticalResult}), e.g. {@code
 * org.aventyrs.core.skill.ataquecorpoacorpo.AtaqueCorpoACorpoCompetencyAbility
 * #ABRIR_DEFESAS}'s Malefício Desprevenido on a critical hit. Deliberately just a
 * marker on top of {@link Effect} for now — no concrete implementation exists yet, and
 * nothing here should be inferred beyond "this is an Effect that is an Efeito Crítico":
 * no critical-detection wiring, no Malefício/status-effect tracking, no duration/Rodada
 * tracking. See {@code org.aventyrs.core.effect} package-info for the pipeline this fits
 * into.
 */
public interface CriticalEffect extends Effect {
}
