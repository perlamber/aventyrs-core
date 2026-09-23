package org.aventyrs.core.action;

/**
 * What just happened that a Reação may answer — the question {@code ReactionOptionsService} filters
 * a combatant's held traits by, and the half of "may I react right now" that is about the
 * <em>event</em> rather than about the reactor.
 *
 * <p>Only the triggers a real trait declares belong here — an enemy moving inside a threatened
 * band, which {@code MovementReactionService} already computes the reactor half of, still has no
 * trait declaring it as its own trigger.
 */
public enum ReactionTrigger {

    /**
     * An ally of the reactor has been named the target of an attack that has not been resolved
     * yet — {@code SantoAbility#GUARDA_VIDAS}'s "apenas quando um aliado for alvo de um ataque".
     */
    ALLY_TARGETED_BY_ATTACK,

    /**
     * The reactor has been named the target of an Ataque Corpo-a-Corpo that has not been resolved
     * yet — {@code FantasmaDoRingueAbility#CRUZ_DE_SANGUE}'s "só pode ser ativada quando você for
     * o alvo único de um ataque corpo-a-corpo". Whether it is the <i>sole</i> target, and whether
     * the attacker stands within reach, are the trait's own questions, read off {@link
     * ReactionContext}.
     */
    SELF_TARGETED_BY_MELEE_ATTACK
}
