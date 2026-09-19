package org.aventyrs.core.skill;

/**
 * The critical outcome of a {@link SkillRoll} — this ruleset's 3d6 rolls a critical off
 * specific dice combinations at the extremes, not just "how many dice matched": three 1s
 * (total 3) is a Falha Crítica Maior; 1+1+2 *specifically* (total 4) is a Falha Crítica
 * Menor — not just "any two 1s", regardless of the third die (e.g. 1+1+5 is a plain failure,
 * not a Falha Crítica Menor; this was a real bug in an earlier version of {@link
 * SkillRoll#getCriticalResult()}, which checked only "two dice show 1", fixed by checking the
 * roll's total instead). Symmetrically, three 6s is an Acerto Crítico Maior — but Acerto
 * Crítico Menor is *not* fixed at one combination the way Falha Crítica Menor is fixed at
 * 1+1+2: it is a <b>Margem Crítica Menor</b>, the 3d6 total a roll has to reach — the number
 * the Armas table prints beside each weapon's Efeito Crítico ({@code Sangramento (17)}, a
 * Florete's {@code (16)}) — and an ability widens it by lowering that total, so {@link
 * SkillRoll#getCriticalResult(int, int)} takes the weapon's own margin and the combined
 * widening as its two parameters. That widening has several real sources: {@code
 * AtaqueCorpoACorpoCompetencyAbility#ATAQUE_PRECISO}, {@link
 * org.aventyrs.core.ability.DexterityAbility#LETALIDADE_PROGRESSIVA}, {@link
 * org.aventyrs.core.ego.SorteAdvantage#ACE}, {@code
 * org.aventyrs.core.skill.artes.ArtesAprimorarComArteAbility}'s "Margem Crítica Menor" branch,
 * and the wielded weapon's own Obras-Primas ({@code OffensiveMasterpiece#DECISIVA}/{@code
 * #MITRAL}).
 *
 * <p>What a critical success adds to the <em>dano</em> roll is {@link
 * org.aventyrs.core.character.CriticalDamage} — the baseline Vantagem em Danos plus whatever
 * the attacker's Talentos and the weapon's Aprimoramentos grant, summed alongside the {@code
 * DamageBonus} rather than folded into it.
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

    /**
     * Whether this is a <b>Menor</b> critical rather than a Maior one — the other axis this enum
     * carries, crossing {@link #isCriticalSuccess()}/{@link #isCriticalFailure()} rather than
     * refining either. An "Efeito Crítico Menor" is one a Menor critical inflicted, and that can
     * arrive from either direction: an {@link #ACERTO_CRITICO_MENOR} on the attacker's roll
     * ({@code AttackDelivery}), or a {@link #FALHA_CRITICA_MENOR} on the defender's own
     * ({@code AttackReceiver}). Both are covered here, which is what lets one filter serve both
     * halves of an exchange.
     *
     * <p>Read by {@code CriticalEffect#applicableTo} for the clauses keyed on severity rather than
     * on which effect landed — {@code MonstruosoFeat#ANATOMIA_UNICA}'s "imune a Efeitos Críticos
     * Menores" and Santo's Despertar.
     */
    public boolean isMinor() {
        return this == ACERTO_CRITICO_MENOR || this == FALHA_CRITICA_MENOR;
    }
}
