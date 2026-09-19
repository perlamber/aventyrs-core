package org.aventyrs.core.action;

/**
 * What just happened that a Reação may answer — the question {@code ReactionOptionsService} filters
 * a combatant's held traits by, and the half of "may I react right now" that is about the
 * <em>event</em> rather than about the reactor.
 *
 * <p>One constant, for the same reason {@link Manoeuvre} has one: only the triggers a real trait
 * declares belong here. A second arrives with the first trait that names it — the obvious candidate
 * being an enemy moving inside a threatened band, which {@code MovementReactionService} already
 * computes the reactor half of, but which no trait declares as its own trigger yet.
 */
public enum ReactionTrigger {

    /**
     * An ally of the reactor has been named the target of an attack that has not been resolved
     * yet — {@code SantoAbility#GUARDA_VIDAS}'s "apenas quando um aliado for alvo de um ataque".
     */
    ALLY_TARGETED_BY_ATTACK
}
