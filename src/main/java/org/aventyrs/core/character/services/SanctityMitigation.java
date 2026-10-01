package org.aventyrs.core.character.services;

import org.aventyrs.core.character.DamageDescriptor;
import org.aventyrs.core.character.DamageSanctity;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.sheet.CombatantSheet;

/**
 * What a sacred or profane hit that lands <b>outside</b> the attack path's {@code DamageService} mitigation is left
 * at (core 0.0.89) — a Corrente's own damage (Toque Sombrio's +1, Veneno Vampírico's 2) or a curse's tick (Definhar's
 * "Dano Físico Profano"): nothing against an immunity to that nature, less a reduction of it. Those stages apply raw
 * damage by design (see {@code effect.Rugido}), so this is the one mitigation they take.
 */
public final class SanctityMitigation {

    private SanctityMitigation() {
    }

    /**
     * amount of type and sanctity, after target's immunity to the nature and its reduction of it. An elemental or
     * untyped type is checked as Primordial, since no element is named.
     */
    public static int apply(final CombatantSheet target, final DamageType type, final DamageSanctity sanctity,
                            final int amount) {
        if (amount <= 0 || sanctity == null) {
            return Math.max(0, amount);
        }
        DamageType checked = type == null || type == DamageType.ELEMENTAL || type == DamageType.FISICO_ELEMENTAL
                ? DamageType.PRIMORDIAL : type;
        if (target.isImmuneToDamage(checked, new DamageDescriptor(checked, null, sanctity))) {
            return 0;
        }
        return Math.max(0, amount - new DamageServiceImpl().getSanctityDamageReduction(target, sanctity));
    }
}
