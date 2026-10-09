package org.aventyrs.core.character.services;

import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.InteractionResult;

/**
 * What one Agarrar roll — a grab, a defence against one, an escape, or a captor keeping hold —
 * came to. Already applied: on success the Agarrado was put on (or taken off) its target before
 * this was returned.
 *
 * @param actionCost what the roller's action cost — reported, never deducted, like every other
 *                   price in this core; {@code null} for a roll that answers someone else's action
 *                   (a defence, a captor keeping hold)
 * @param total      the roll's total — the faces plus everything the roll resolved
 * @param required   the GD or Defesa it had to reach
 * @param succeeded  whether the roller got what they wanted: the grab took hold, the defence held,
 *                   the escape worked, the hold was kept
 * @param rollResult the underlying Perícia roll, for its breakdown and its critical result
 */
public record GrappleResult(ActionCost actionCost, int total, int required, boolean succeeded,
                            InteractionResult rollResult) {
}
