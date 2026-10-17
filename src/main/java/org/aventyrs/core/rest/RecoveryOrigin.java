package org.aventyrs.core.rest;

/**
 * Whether a Descanso-shaped recovery is a <b>real Descanso</b> or an effect that merely recovers
 * "como se passasse por um Descanso" — a Magia de cura (SpellHealingEffect, VidaSpell), Beijo de
 * Boros, an Autocontrole spend, Medicina, a monster aura's heal.
 *
 * <p>Table ruling (2026-10-16): only a real Descanso pays the Descanso bonuses its holder's traits
 * add ({@code AttributeAbility#resolveRest*Bonus}, {@code Feat#resolveRest*Bonus} — Metabolismo
 * Rápido's +3PV, Canalizador de Mana's +2PM, Supermotivado's PD). An equivalent recovers the
 * Atributo × tier base plus only the hooks written for any recovery effect ({@code
 * #resolveRestEquivalent*Bonus}); a heal from a Magia or Título reaches Metabolismo's own +2PV
 * through the heal funnel instead.
 */
public enum RecoveryOrigin {
    /** A Descanso actually taken — {@code RestService#applyRest}. */
    DESCANSO,
    /** An effect recovering as a Descanso of some tier would. */
    EQUIVALENTE
}
