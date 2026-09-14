package org.aventyrs.core.magic;

/**
 * When a {@link SpellHealing} actually heals — the live-state gate on a Magia's recovery, the
 * same shape {@code org.aventyrs.core.item.FavorCondition} is for an item's Favor.
 *
 * <p>Only one Magia needs a gate today, so there are only two constants; a third is added when a
 * second real clause asks for one, per CLAUDE.md's "build for the second real consumer".
 */
public enum HealingCondition {

    /** No gate — the Magia heals whatever the target's state. */
    ALWAYS,

    /**
     * <i>Aliviar a Dor</i>'s "Interrompe qualquer efeito de Sangramento sofrido pelo alvo, se ele
     * não estiver sob sangramento, <b>ao invés disso</b> essa magia cura o alvo". The two are
     * alternatives rather than both happening: a bleeding target gets the bleeding stopped and no
     * PV back, and only a target who was not bleeding is healed.
     *
     * <p>Note this is not the same as the interruption {@code CombatantSheet#heal} performs for
     * free — that clears a {@code Bleeding} <i>because</i> healing happened, whereas here
     * stopping the bleeding is what happens <i>instead of</i> healing.
     */
    ONLY_IF_NOT_BLEEDING
}
