package org.aventyrs.core.skill;

/**
 * The critical outcome of a {@link SkillRoll} — this ruleset's 3d6 rolls a critical off
 * specific dice combinations at the extremes, not just "how many dice matched": three 1s
 * (total 3) is a Falha Crítica Maior; 1+1+2 *specifically* (total 4) is a Falha Crítica
 * Menor — not just "any two 1s", regardless of the third die (e.g. 1+1+5 is a plain failure,
 * not a Falha Crítica Menor; this was a real bug in an earlier version of {@link
 * SkillRoll#getCriticalResult()}, which checked only "two dice show 1", fixed by checking the
 * roll's total instead). Symmetrically, three 6s is an Acerto Crítico Maior — but Acerto
 * Crítico Menor is *not* fixed at "6+6+5" the way Falha Crítica Menor is fixed at 1+1+2:
 * abilities like {@code AtaqueCorpoACorpoCompetencyAbility#ATAQUE_PRECISO} widen its margin
 * (e.g. 5s counting alongside 6s), so a fixed-sum check would go stale the moment that
 * widening is wired in — see the {@code TODO} on {@link SkillRoll#getCriticalResult()} itself,
 * which (deliberately, for now) still has the same "two 6s, any third die" bug the Falha
 * Crítica Menor side just had fixed, until a real margin-widening mechanism exists to fix it
 * properly rather than just swapping one fixed threshold for another.
 *
 * <p>Abilities like {@code ATAQUE_PRECISO} above are also expected to eventually widen which
 * face values count toward a Menor result on the *dano* side, not just this roll's own
 * critical detection — nothing consumes either yet.
 */
public enum CriticalResult {
    NONE,
    FALHA_CRITICA_MENOR,
    FALHA_CRITICA_MAIOR,
    ACERTO_CRITICO_MENOR,
    ACERTO_CRITICO_MAIOR
}
