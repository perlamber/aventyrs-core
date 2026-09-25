package org.aventyrs.core.effect;

import org.aventyrs.core.sheet.CombatantSheet;

/**
 * The per-cast facts a {@link SpellEffectBuilder} needs on top of the Magia's own authored
 * columns — everything that varies between two casts of the same Magia.
 *
 * <p>A record rather than a parameter list because the set will grow and the signature should
 * not: an {@link OffensiveEffect} will want the caster, and {@code Sobrecura} needs a caster
 * <em>and</em> an already-rolled d6 (this core rolls none). A bare {@code boolean} carries
 * neither, and widening every builder's signature each time a category lands is the churn this
 * avoids.
 *
 * @param hostileTarget whether the target counts as an enemy of the caster — {@code
 *                      VidaSpell#NOVA_REJUVENESCEDORA}'s "Inimigos do conjurador recuperam apenas
 *                      metade". The caller's to say: this core resolves no Área de Efeito
 *                      footprint, so a caller sweeping an area builds one effect per combatant
 * @param caster        who is casting, or {@code null} when the caller did not say — what a heal
 *                      needs to ask the caster's Títulos about healing the fallen (Levantar os
 *                      Caídos, Curar os Mortos); {@code null} reads as "no such Título"
 */
public record SpellEffectContext(boolean hostileTarget, CombatantSheet caster) {

    /** The ordinary case — a Magia cast on someone who is not an enemy of its caster. */
    public static final SpellEffectContext FRIENDLY = new SpellEffectContext(false, null);

    /** A target hostile to the caster, for a Magia that gives an enemy less than an ally. */
    public static final SpellEffectContext HOSTILE = new SpellEffectContext(true, null);

    /** {@link #HOSTILE} or {@link #FRIENDLY}, whichever hostile says. */
    public static SpellEffectContext of(final boolean hostile) {
        return hostile ? HOSTILE : FRIENDLY;
    }

    /** {@link #of(boolean)} naming the caster; a {@code null} caster is that same constant. */
    public static SpellEffectContext of(final boolean hostile, final CombatantSheet caster) {
        return caster == null ? of(hostile) : new SpellEffectContext(hostile, caster);
    }
}
