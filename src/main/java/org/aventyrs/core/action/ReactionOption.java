package org.aventyrs.core.action;

import org.aventyrs.core.scene.Teleportation;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.title.AventyrTitleAbility;
import org.aventyrs.core.title.PDCost;

/**
 * One Reação a combatant may take right now — what {@code ReactionOptionsService} returns and a
 * client renders as a menu entry, with everything that menu needs already resolved so it never has
 * to ask the ability itself.
 *
 * <p>{@link #affordable} is reported rather than filtered on: an option the reactor cannot pay for
 * is still an option they <em>have</em>, and a client shows it greyed out instead of hiding a
 * Suprema the player knows they own. Filtering it here would make the list lie about what the
 * character can do.
 *
 * @param ability       the trait this Reação comes from
 * @param cost          its Tempo de Ativação — always {@code ActionCost.REACTION} today, carried
 *                      anyway so a client renders one shape for every kind of option
 * @param determinationCost the PD the activation costs, straight off the ability
 * @param teleportation where activating it would move the reactor, or {@code null} — the reach a
 *                      client needs to draw the destination before the player commits
 * @param affordable    whether the reactor is entitled to a Reação at all <em>and</em> can pay the
 *                      PD right now
 */
public record ReactionOption(AventyrTitleAbility ability, ActionCost cost, PDCost determinationCost,
                             Teleportation teleportation, boolean affordable) {
}
