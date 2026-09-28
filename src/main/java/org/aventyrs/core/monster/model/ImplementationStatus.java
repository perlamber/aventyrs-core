package org.aventyrs.core.monster.model;

/**
 * How much of a Habilidade Monstruosa this core applies — so an editor can say so, and a table
 * knows what it still runs by hand.
 */
public enum ImplementationStatus {

    /** Every clause is applied. */
    APPLIED,

    /**
     * Its numbers are applied; some clause needs a system this core lacks (geometry, summons,
     * possession, a race swap …). {@code MonstrousAbility#getUnappliedNote()} names which.
     */
    PARTIAL,

    /** Held and counted (+2PV, budgets) but none of its effects are applied. */
    TABLE_ONLY
}
