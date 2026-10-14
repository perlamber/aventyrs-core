package org.aventyrs.core.character;

import org.aventyrs.core.magic.ElementalType;
import org.aventyrs.core.sheet.IllegalOperationException;

import lombok.NonNull;

import static org.aventyrs.core.util.TranslatableMessages.INVALID_DAMAGE_TYPE_ELEMENT_PAIRING;

/**
 * The complete type of one incoming damage instance: its {@link DamageType} nature plus the qualifier the 2026-10-12
 * taxonomy names it by — an {@link ElementalType} so a resistance can distinguish Fogo from Gelo, or a {@link
 * DamageSanctity}.
 *
 * <p>Validated at construction, because an incoming attack's classification is input from outside this core:
 * {@code elementalType} is required for {@link DamageType#ELEMENTAL}, allowed for {@link DamageType#MAGICO} (Mágico
 * Elemental) and prohibited otherwise; a {@link DamageType#MAGICO} hit must carry an element or a sanctity — there is
 * no plain Dano Mágico (table ruling).
 */
public record DamageDescriptor(@NonNull DamageType damageType, ElementalType elementalType, DamageSanctity sanctity) {

    public DamageDescriptor(@NonNull final DamageType damageType) {
        this(damageType, null, null);
    }

    /** A hit with no sacred or profane nature — every descriptor before core 0.0.89. */
    public DamageDescriptor(@NonNull final DamageType damageType, final ElementalType elementalType) {
        this(damageType, elementalType, null);
    }

    /** This hit with its nature set to sanctity ({@code null} clears it) — "Profano em substituição aos seus tipos". */
    public DamageDescriptor withSanctity(final DamageSanctity sanctity) {
        return new DamageDescriptor(damageType, elementalType, sanctity);
    }

    public DamageDescriptor {
        if (elementalType == ElementalType.TODOS
                || (damageType.requiresElement() && elementalType == null)
                || (!damageType.admitsElement() && elementalType != null)
                || (damageType == DamageType.MAGICO && elementalType == null && sanctity == null)) {
            throw new IllegalOperationException(INVALID_DAMAGE_TYPE_ELEMENT_PAIRING);
        }
    }
}
