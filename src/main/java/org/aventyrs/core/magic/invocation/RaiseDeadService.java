package org.aventyrs.core.magic.invocation;

import org.aventyrs.core.scene.Scene;
import org.aventyrs.core.sheet.CombatantSheet;

/**
 * Which bodies Reanimar and Criar Carniçal may touch ("um cadáver", core 0.1.6.24, table rulings 2026-10-14):
 *
 * <ul>
 *   <li>a dead combatant (status {@code DEAD}) — a player character included;</li>
 *   <li>a fallen undead (≤ 0 PV with the Anatomia de Morto-Vivo, or {@code isUndead()}), or a {@link
 *   CombatantSheet#isCorpse() corpse} an earlier raise left behind;</li>
 *   <li>never one raised {@link #MAX_RAISES} times already, one marked beyond revival, or a living invocação.</li>
 * </ul>
 */
public interface RaiseDeadService {

    /** "A body can be raised 4x as an undead, once the last time it can't be raised again." */
    int MAX_RAISES = 4;

    /** Whether corpse is a body that may be raised in scene. */
    boolean isRaisable(Scene scene, CombatantSheet corpse);

    /**
     * Refuses a body that may not be raised.
     *
     * @throws org.aventyrs.core.sheet.IllegalOperationException {@code RAISE_REQUIRES_CORPSE}, or {@code
     *                                                           CORPSE_EXHAUSTED} for a body raised {@link #MAX_RAISES}
     *                                                           times
     */
    void requireRaisable(Scene scene, CombatantSheet corpse);
}
