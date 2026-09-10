package org.aventyrs.core.character;

import org.aventyrs.core.magic.ElementalType;
import org.aventyrs.core.sheet.IllegalOperationException;

import lombok.NonNull;

import static org.aventyrs.core.util.TranslatableMessages.INVALID_DAMAGE_TYPE_ELEMENT_PAIRING;

/**
 * The complete type of one incoming damage instance. {@link DamageType} identifies the broad
 * category; an elemental category also names the exact {@link ElementalType} so a resistance can
 * distinguish Fogo from Gelo.
 *
 * <p>{@code elementalType} is required exactly for {@link DamageType#ELEMENTAL} and {@link
 * DamageType#FISICO_ELEMENTAL}, and prohibited for every other damage type. This is validated at
 * construction because an incoming attack's classification is input from outside this core.
 */
public record DamageDescriptor(@NonNull DamageType damageType, ElementalType elementalType) {

    public DamageDescriptor(@NonNull final DamageType damageType) {
        this(damageType, null);
    }

    public DamageDescriptor {
        boolean elementalDamage = damageType == DamageType.ELEMENTAL
                || damageType == DamageType.FISICO_ELEMENTAL;
        if (elementalDamage == (elementalType == null) || elementalType == ElementalType.TODOS) {
            throw new IllegalOperationException(INVALID_DAMAGE_TYPE_ELEMENT_PAIRING);
        }
    }
}
