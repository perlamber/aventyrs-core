package org.aventyrs.core.effect;

/**
 * What one {@link DefensiveCriticalEffect} did, and what it leaves the caller to do. Whatever could
 * be applied to the two sheets in hand already was; the rest is geometry, a second attack, or a
 * Magia — none of which this core resolves on its own.
 *
 * @param type                     which effect this was
 * @param damageToAttacker         PV the attacker lost (already applied), 0 when none
 * @param attackerPushedUd         UD the caller pushes the attacker back, 0 when none
 * @param defenderMayMoveUd        UD the defender may move freely (ignoring Terreno Difícil)
 * @param counterAttack            the defender may counter-attack the attacker now
 * @param counterAttackMinorCritical that counter-attack applies the defender's weapon's Efeito
 *                                 Crítico Menor ("aplica seu Efeito Crítico Menor")
 * @param quickCast                what the defender may cast immediately — {@link QuickCast#NONE}
 *                                 when nothing
 * @param attackerCastingSuppressed a repelled Magia's caster is kept from casting (Repelir e
 *                                 Suprimir) — for the caller to enforce on that caster's client
 */
public record DefensiveCriticalOutcome(DefensiveCriticalEffectType type,
                                       int damageToAttacker,
                                       int attackerPushedUd,
                                       int defenderMayMoveUd,
                                       boolean counterAttack,
                                       boolean counterAttackMinorCritical,
                                       QuickCast quickCast,
                                       boolean attackerCastingSuppressed) {

    /** Surto Arcano's permission. */
    public enum QuickCast {
        NONE,
        /** Menor: "uma Magia Semente ou Broto". */
        SEED_OR_BUD,
        /** Maior: "uma qualquer". */
        ANY
    }

    /** An outcome of type with nothing further to do. */
    public static DefensiveCriticalOutcome of(final DefensiveCriticalEffectType type) {
        return new DefensiveCriticalOutcome(type, 0, 0, 0, false, false, QuickCast.NONE, false);
    }
}
