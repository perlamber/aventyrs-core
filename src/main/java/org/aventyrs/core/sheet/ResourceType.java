package org.aventyrs.core.sheet;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.modifier.ModifierType;

/**
 * A CombatantSheet's three resource pools — and, since core 0.1.0, the two facts that make each one's maximum: the
 * Atributo it scales with and the {@link ModifierType} that moves its Multiplicador. Every pool follows the same
 * formula, {@code base + Atributo × Multiplicador (+ flat bonus)}, resolved in one place — {@code
 * character.services.ResourcePoolFormula}.
 */
public enum ResourceType {
    HIT_POINTS(AttributeDomain.VIGOR, ModifierType.LIFE_MULTIPLIER),
    MAGIC_POINTS(AttributeDomain.FOCUS, ModifierType.MANA_MULTIPLIER),
    DETERMINATION_POINTS(AttributeDomain.INSTINCT, ModifierType.DETERMINATION_MULTIPLIER);

    private final AttributeDomain governingAttribute;
    private final ModifierType multiplierModifier;

    ResourceType(final AttributeDomain governingAttribute, final ModifierType multiplierModifier) {
        this.governingAttribute = governingAttribute;
        this.multiplierModifier = multiplierModifier;
    }

    /** The Atributo this pool scales with — Vigor for PV, Foco for PM, Instinto for PD. */
    public AttributeDomain getGoverningAttribute() {
        return governingAttribute;
    }

    /** The {@link ModifierType} a trait, a Condição or a timed bonus moves this pool's Multiplicador with. */
    public ModifierType getMultiplierModifier() {
        return multiplierModifier;
    }
}
