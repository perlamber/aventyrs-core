package org.aventyrs.core.scene;

import org.aventyrs.core.sheet.CombatantAction;
import org.aventyrs.core.sheet.CombatantSheet;

/**
 * One {@link CombatantAction} as it sits in a {@link Scene}'s permanent history — the action
 * paired with the combatant that took it. The per-combatant logs on {@link CombatantSheet}
 * ({@link CombatantSheet#getActionsThisRound()}/{@link CombatantSheet#getActionsThisCena()})
 * don't record who acted because each of those belongs to exactly one sheet already; a
 * Scene-wide history the client renders as a combat log does need it.
 *
 * <p>Appended by {@link Scene#recordAction(CombatantSheet, CombatantAction)} and never cleared
 * — unlike the per-combatant logs, which reset at the Rodada/Cena boundary. The Rodada each
 * action was taken in is on {@link CombatantAction#turnNumber()}, and the ordering of this
 * list is the order the actions were recorded.
 *
 * @param combatant the sheet that took the action — recognised elsewhere by {@link
 *                  CombatantSheet#getId()}, the same way {@link Scene} keys every other
 *                  participant reference it holds.
 * @param action    what was done — see {@link CombatantAction}.
 */
public record SceneAction(CombatantSheet combatant, CombatantAction action) {
}
