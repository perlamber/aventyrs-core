package org.aventyrs.core.action;

import org.aventyrs.core.magic.Spell;
import org.aventyrs.core.sheet.ActionCost;

/**
 * One of the Reações every character has, as a reactor may take it right now (core 0.1.6.17) — the generic twin of
 * {@link ReactionOption}. Unaffordable ones are reported, not filtered, for the same reason.
 *
 * @param reaction   which Reação
 * @param spell      for {@link GenericReaction#CONJURACAO_REATIVA}, the Magia; {@code null} otherwise
 * @param cost       always {@link ActionCost#REACTION}
 * @param affordable whether the reactor may spend the Reação it needs — the ordinary one, or a Preparação's own
 */
public record GenericReactionOption(GenericReaction reaction, Spell spell, ActionCost cost, boolean affordable) {
}
