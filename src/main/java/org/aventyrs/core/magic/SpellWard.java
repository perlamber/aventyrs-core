package org.aventyrs.core.magic;

/**
 * A protection a Magia leaves on its target, held on the sheet (core 0.0.94) — {@link Spell#getWard()}, built by {@code
 * effect.SpellWardEffect}.
 */
public enum SpellWard {

    /** Corpo Fechado: "se torna imune à Malefícios" — a {@code sheet.MaleficioWard}, held by the caster's Concentração. */
    MALEFICIOS,

    /** Procrastinar Ferimento: the next hit's PV are lost a Rodada later — a {@code sheet.PostponedWoundWard}. */
    POSTPONED_WOUND
}
