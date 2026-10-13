package org.aventyrs.core.sheet;

import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.magic.ElementalType;

/**
 * The last hit a combatant took on the attack path — its final (mitigated) damage and its type,
 * recorded by {@code DamageInteraction} before any Efeito Crítico behind it in the same chain runs.
 * What an Efeito Crítico reading "o dano … causado por este ataque" reads: Cataclismo's "dano
 * Elemental", Estilhaçador's "metade do dano sofrido", Oferenda Maldita's Roubo de Vida.
 *
 * @param damageType   {@code null} for an untyped hit
 * @param elementalType the element the hit named, or {@code null}
 * @param source       who dealt it, or {@code null}
 */
public record DamageReceipt(int damage, DamageType damageType, ElementalType elementalType, CombatantSheet source) {

    /** A receipt naming no element. */
    public DamageReceipt(final int damage, final DamageType damageType, final CombatantSheet source) {
        this(damage, damageType, null, source);
    }

    /** Whether this hit was elemental — an Elemental or a Mágico Elemental (2026-10-12 taxonomy). */
    public boolean isElemental() {
        return damageType == DamageType.ELEMENTAL || elementalType != null;
    }
}
