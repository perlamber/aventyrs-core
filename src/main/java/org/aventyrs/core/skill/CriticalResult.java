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
 * (e.g. 5s counting alongside 6s), so {@link SkillRoll#getCriticalResult(int)} takes the
 * combined widening as a parameter instead of using a fixed threshold — see that method's own
 * javadoc for how the two matching dice are found once the required face value is lowered.
 * {@code ATAQUE_PRECISO} itself is still TODO'd (it needs its own enum/Interaction wiring), but
 * the margin-widening mechanism it was blocked on now exists — {@link
 * org.aventyrs.core.ability.DexterityAbility#LETALIDADE_PROGRESSIVA}, {@link
 * org.aventyrs.core.ego.SorteAdvantage#ACE}, and {@code
 * org.aventyrs.core.skill.artes.ArtesAprimorarComArteAbility}'s "Margem Crítica Menor" branch
 * are the first real sources feeding it.
 *
 * <p>Abilities like {@code ATAQUE_PRECISO} above are also expected to eventually widen which
 * face values count toward a Menor result on the *dano* side, not just this roll's own
 * critical detection — nothing consumes that half yet.
 */
public enum CriticalResult {
    NONE,
    FALHA_CRITICA_MENOR,
    FALHA_CRITICA_MAIOR,
    ACERTO_CRITICO_MENOR,
    ACERTO_CRITICO_MAIOR;

    /**
     * Whether this is a critical success (Menor or Maior) rather than a plain roll or a
     * critical failure — e.g. what {@link AbstractSkillInteraction} gates its {@code
     * AttributeAbility#resolveCriticalSuccessEgoGain} scan on, so that scan only runs on the
     * roll outcome it can ever actually apply to.
     */
    public boolean isCriticalSuccess() {
        return this == ACERTO_CRITICO_MENOR || this == ACERTO_CRITICO_MAIOR;
    }

    /**
     * Whether this is a critical failure (Menor or Maior) — the symmetric counterpart to {@link
     * #isCriticalSuccess()}. Its first consumer is {@code
     * org.aventyrs.core.combat.AttackReceiver}: an incoming attack is resolved as the
     * <i>defender's</i> roll, so it's a critical <i>failure</i> on that roll that means the
     * attacker landed a critical hit and this attack's Efeitos Críticos fire.
     */
    public boolean isCriticalFailure() {
        return this == FALHA_CRITICA_MENOR || this == FALHA_CRITICA_MAIOR;
    }
}
