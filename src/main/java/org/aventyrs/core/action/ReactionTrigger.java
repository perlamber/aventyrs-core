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
    SELF_TARGETED_BY_MELEE_ATTACK,

    /**
     * The reactor was just hit by a successful attack — its Defesa failed. Frenesi Reativo: "Você pode
     * iniciar seu Frenesi em resposta às ações inimigas, mas apenas se você for alvo de um ataque
     * bem-sucedido". Offered after the Defesa, before the next action.
     */
    SELF_HIT_BY_SUCCESSFUL_ATTACK,

    /**
     * An enemy adjacent to the reactor attacked someone else — Retaliação Furiosa: "Sempre que um
     * Personagem Inimigo adjacente atacar outros personagens que não você". {@code ReactionContext
     * #getAttacker()} is that enemy.
     */
    ADJACENT_ENEMY_ATTACKS_OTHER,

    /**
     * Damage about to land would take the reactor from above 0 PV to 0 or below — Fanático de Cyt:
     * "se um ataque ou efeito for reduzir seus PV para zero ou menos". Offered <em>before</em> the
     * damage is applied; {@code ReactionContext#getPendingDamage()} carries it.
     */
    SELF_WOULD_DROP_TO_ZERO_HP
}
