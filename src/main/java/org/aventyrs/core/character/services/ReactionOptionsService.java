package org.aventyrs.core.character.services;

import org.aventyrs.core.action.ReactionContext;
import org.aventyrs.core.action.ReactionOption;

import java.util.List;

/**
 * Which Reações a combatant may take right now — the menu a client renders when something happens
 * on somebody else's Turn. The counterpart to {@code ReactionsService}, which answers "how many
 * Reações do they have" but never "what could they spend one on", and to {@code
 * MovementReactionService}, which answers "who may react to this mover" but never "with what".
 *
 * <p><b>It reports the opportunity; it never fires or spends anything.</b> Same disclaimer {@code
 * MovementReactionService} carries, and for the same reason: nothing in this core counts a Reação
 * as spent, so a combatant who has already reacted this Rodada is still offered their Reações. The
 * caller adjudicates, then activates through {@code AventyrTitle#activateAbility} — which re-runs
 * every gate for real, since a listed option is a snapshot and the world moves between looking and
 * committing.
 *
 * <p>Three things it deliberately does not do:
 *
 * <ul>
 *   <li><b>It never guesses at a missing context.</b> A {@code null} {@code
 *       ReactionContext#getReactorContext()} yields an empty list — "cannot tell" is nobody, not
 *       everybody, the same rule {@code MovementReactionService} follows.</li>
 *   <li><b>It never hides an unaffordable option.</b> One the reactor cannot pay for comes back
 *       with {@code ReactionOption#affordable()} false rather than being filtered out — see that
 *       record's own javadoc.</li>
 *   <li><b>It scans Habilidades de Título only.</b> That is the only trait type with a Reação
 *       today. An {@code ActiveAbility} <em>could</em> declare {@code ActionCost.REACTION} since
 *       {@code ActionCost} became the Tempo de Ativação type, but none does, and a {@code Feat}/
 *       {@code AttributeAbility}/{@code EgoAdvantage} has no reaction concept at all. Widening the
 *       scan is a few lines when the first real one appears; doing it now would be building for a
 *       consumer that does not exist.</li>
 * </ul>
 */
public interface ReactionOptionsService {

    /**
     * Every Reação context's reactor may take in answer to its trigger, affordable or not, in
     * catalog order.
     *
     * @return an empty list when the reactor holds no matching Reação, or when the context carries
     *         no {@code SceneContext} to measure against
     */
    List<ReactionOption> getAvailableReactions(ReactionContext context);
}
