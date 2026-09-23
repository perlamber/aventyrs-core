package org.aventyrs.core.sheet;

import org.aventyrs.core.character.DamageType;

/**
 * The last hit a combatant took on the attack path — its final (mitigated) damage and its type,
 * recorded by {@code DamageInteraction} before any Efeito Crítico behind it in the same chain runs.
 * What an Efeito Crítico reading "o dano … causado por este ataque" reads: Cataclismo's "dano
 * Elemental", Estilhaçador's "metade do dano sofrido", Oferenda Maldita's Roubo de Vida.
 *
 * @param damageType {@code null} for an untyped hit
 * @param source     who dealt it, or {@code null}
 */
public record DamageReceipt(int damage, DamageType damageType, CombatantSheet source) {

    /** Whether this hit was elemental — Físico Elemental or Elemental. */
    public boolean isElemental() {
        return damageType == DamageType.ELEMENTAL || damageType == DamageType.FISICO_ELEMENTAL;
    }
}
