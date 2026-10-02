package org.aventyrs.core.skill;

/**
 * How often a limited-use Habilidade de Competência renews — read by {@link CompetencyUses}, which keeps each
 * window on the sheet ledger that already resets at that boundary.
 */
public enum UseWindow {

    /** "uma vez por Rodada" — {@code CombatantSheet#spendRoundScopedUse}, reset each Rodada. */
    ROUND,

    /**
     * "uma vez por Cena" — {@code CombatantSheet#incrementCombatCounter}. ⚠️ Kept per combat (reset by {@code
     * endCombat} and {@code startNewScene}), the house reading every per-Cena clause in this core already uses.
     */
    CENA,

    /** "a cada dia … renovado após passar por um Descanso Longo" — {@code CombatantSheet#spendRestScopedUse}. */
    LONG_REST
}
