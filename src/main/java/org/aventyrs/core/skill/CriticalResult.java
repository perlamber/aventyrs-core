package org.aventyrs.core.skill;

/**
 * The critical outcome of a {@link SkillRoll} — this ruleset's 3d6 rolls a critical off
 * matching dice at the extremes, not the total: three dice showing 1 is a Falha Crítica
 * Maior, two is a Falha Crítica Menor; symmetrically, three 6s is an Acerto Crítico Maior,
 * two is an Acerto Crítico Menor (this second half — the "two 6s" case — is inferred by
 * symmetry with the confirmed "two/three 1s" failure case, not itself independently
 * confirmed against rules text; revisit if that turns out wrong). See {@link
 * SkillRoll#getCriticalResult()} for the actual detection.
 *
 * <p>Abilities like {@code AtaqueCorpoACorpoCompetencyAbility#ATAQUE_PRECISO} ("a margem
 * crítica menor... é aumentada em +1 número") are expected to eventually widen which face
 * values count toward a Menor result (e.g. 5s counting alongside 6s for Acerto Crítico
 * Menor) — nothing consumes that yet; {@link SkillRoll#getCriticalResult()} only checks the
 * fixed 1/6 extremes for now.
 */
public enum CriticalResult {
    NONE,
    FALHA_CRITICA_MENOR,
    FALHA_CRITICA_MAIOR,
    ACERTO_CRITICO_MENOR,
    ACERTO_CRITICO_MAIOR
}
