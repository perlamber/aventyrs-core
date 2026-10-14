package org.aventyrs.core.action;

import java.util.Set;

/**
 * The Reações every character has, whatever they hold — "Manobras de Combate - Reações" (acoes-de-combate.txt
 * 20261013, core 0.1.6.17). Each answers its own triggers; {@code ReactionOptionsService#getGenericReactions} lists
 * the ones a reactor may take right now, beside the Título traits it lists already.
 */
public enum GenericReaction {

    /** "Permite atacar um inimigo que se movimente em sua Área de Ataque" — an Ataque Corpo-a-Corpo roll. */
    DEFENDER_O_PERIMETRO(Set.of(ReactionTrigger.ENEMY_MOVES_IN_REACH, ReactionTrigger.PROVOKING_MANOEUVRE)),

    /**
     * "Permite atacar um inimigo que inicie uma Conjuração ou Mimetismo de Magias dentro de sua Área de Ataque" — an
     * Ataque Corpo-a-Corpo; on a hit, -2 on the casting roll per 5 points of damage taken
     * ({@code SpellCastRequest#getDisruptionDamage}).
     */
    IMPEDIR_CONJURACAO(Set.of(ReactionTrigger.ENEMY_CASTS_IN_REACH)),

    /**
     * "Você pode agir como escudo humano de um aliado" — once per combat, an adjacent ally attacked; the attack is
     * intercepted ({@code IncomingAttack#isIntercepted}/{@code DeliveredAttack#isIntercepted}).
     */
    SACRIFICIO(Set.of(ReactionTrigger.ALLY_TARGETED_BY_ATTACK)),

    /** "Conforme Magia Conjurada" — a Magia whose Tempo de Conjuração is a Reação, on any trigger. */
    CONJURACAO_REATIVA(Set.of(ReactionTrigger.values())),

    /**
     * "Conforme Habilidade Monstruosa ou de Título Aventyr" — the Título traits are the {@code ReactionOption}s
     * {@code ReactionOptionsService#getAvailableReactions} lists; this constant names the family.
     */
    ATIVACAO_REATIVA(Set.of(ReactionTrigger.values())),

    /**
     * Firing a Preparação — the action named when it was prepared, on any trigger, with the extra Reação the
     * Preparação granted ({@code PreparationService}).
     */
    PREPARACAO(Set.of(ReactionTrigger.values()));

    private final Set<ReactionTrigger> triggers;

    GenericReaction(final Set<ReactionTrigger> triggers) {
        this.triggers = triggers;
    }

    /** Whether this Reação answers trigger. */
    public boolean answers(final ReactionTrigger trigger) {
        return triggers.contains(trigger);
    }
}
