package org.aventyrs.core.effect;

import lombok.NonNull;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.InteractionResult;

/**
 * Inquisição — Benção Elduriana's Corrente: "GD de Magias do Alvo +2 níveis." The target's Magias are {@link
 * #DIFFICULTY_STEPS} níveis harder to cast until the attacker's next Turn starts (table ruling, 2026-10-12), held
 * on the target ({@code CombatantSheet#markSpellDifficultyIncrease}) and read by {@code SpellCastingService}.
 * Re-applied by the same attacker it refreshes, never stacks.
 */
public class Inquisicao extends AbstractEffect implements EffectChain {

    /** "+2 níveis". */
    public static final int DIFFICULTY_STEPS = 2;

    private final CombatantSheet attacker;

    /** @param attacker whose next Turn ends the mark */
    public Inquisicao(@NonNull final CombatantSheet attacker) {
        this.attacker = attacker;
    }

    public CombatantSheet getAttacker() {
        return attacker;
    }

    @Override
    public String getDescription() {
        return "Inquisição: GD de Magias do Alvo +2 níveis.";
    }

    @Override
    public InteractionResult applyTo(final CombatantSheet target) {
        target.markSpellDifficultyIncrease(attacker, DIFFICULTY_STEPS);
        return reportChain(InteractionResult.builder().resultStatus(resolveStatus(target))).build();
    }
}
