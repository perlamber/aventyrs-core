package org.aventyrs.core.sheet;

/**
 * What kind of action a Preparação holds (core 0.1.6.21) — "Rolagem de Perícia específica, Ativação de Habilidade,
 * Conjuração de Magia, etc.". The kind tells the caller which of its own paths runs the action when it is fired; the
 * {@link PreparedAction#getKey() key} names the action within that kind. A plain Perícia roll is not one: Perícias are
 * rolled only when the Narrador asks (table ruling), so a Perícia is prepared through an attack or a competency action.
 */
public enum PreparedActionKind {
    /** A Habilidade de Título. */
    TITLE_ABILITY,
    /** A Habilidade Ativa that is neither a Título's nor a monster's. */
    ACTIVE_ABILITY,
    /** A Habilidade Monstruosa. */
    MONSTROUS_ABILITY,
    /** A learned Magia — or its Efeito Alternativo. */
    SPELL,
    /** A Magia Mimetizada. */
    MIMETIZED_SPELL,
    /** An attack — a Perícia de Ataque roll with a weapon or a natural attack. */
    ATTACK,
    /** A Habilidade de Competência that is an action of its own ({@code CompetencyActions}). */
    COMPETENCY_ACTION
}
