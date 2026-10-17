package org.aventyrs.core.character.services;

import org.aventyrs.core.scene.Scene;
import org.aventyrs.core.sheet.CombatantSheet;

/**
 * Paying for a Rodada 0.0 action — {@code InstinctAbility#PARANOIA_SAUDAVEL}'s "Ao custo de 2PD e apenas uma vez a
 * cada Cena" (core 0.1.7.15; table ruling 2026-10-16: a phase before Rodada 0, once per combat). Holding the action
 * itself is {@link Scene#beginPreInitiativeTurn}'s.
 */
public interface PreInitiativeService {

    /** The PD a Rodada 0.0 action costs sheet — the lowest a held Habilidade states — or 0 when none offers one. */
    int getCost(CombatantSheet sheet);

    /**
     * Whether sheet may claim a Rodada 0.0 action in scene now: scene is at its combat start ({@link
     * Scene#isAtCombatStart}, or {@code null} for a caller holding no live Scene), a Habilidade offers one, it has
     * not been taken this combat, and sheet can pay the PD.
     */
    boolean canTake(CombatantSheet sheet, Scene scene);

    /**
     * Spends the PD and marks the use for the combat.
     *
     * @return the PD spent
     * @throws org.aventyrs.core.sheet.IllegalOperationException ({@code PRE_INITIATIVE_ACTION_UNAVAILABLE}) when
     *         {@link #canTake} is false
     */
    int take(CombatantSheet sheet, Scene scene);
}
