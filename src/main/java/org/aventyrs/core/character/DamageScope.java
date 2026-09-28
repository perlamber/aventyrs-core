package org.aventyrs.core.character;

import lombok.NonNull;
import org.aventyrs.core.magic.ElementalType;

import java.util.Set;

/**
 * Which damage a resistance clause reaches — "Danos do Elemento escolhido", "danos físicos",
 * "Danos Mágicos", "danos que não sejam Elementais ou Primordiais". Read by the Meio-Dano,
 * immunity and vulnerability hooks ({@code AttributeAbility#halvesDamage} and siblings, {@code
 * sheet.DamageScopeEffect}).
 *
 * <p>An <b>element</b> scope can only match a hit that arrives with a {@link DamageDescriptor} —
 * a bare {@link DamageType} carries no element, so {@code DamageInteraction}'s type-only path
 * (the ordinary attack) never matches one. That is the same limit RE already has.
 *
 * @param types   the {@link DamageType}s it reaches
 * @param element the element it is limited to, or {@code null} for any; {@link ElementalType#TODOS}
 *                matches every element
 */
public record DamageScope(@NonNull Set<DamageType> types, ElementalType element) {

    /** Every hit. */
    public static final DamageScope ALL = new DamageScope(Set.of(DamageType.values()), null);

    /** "Danos Físicos" — a plain physical hit, elemental or not. */
    public static final DamageScope PHYSICAL = new DamageScope(Set.of(DamageType.FISICO, DamageType.FISICO_ELEMENTAL), null);

    /** "Danos Mágicos". */
    public static final DamageScope MAGICAL = new DamageScope(Set.of(DamageType.MAGICO), null);

    /** "Efeitos e danos que não sejam Elementais ou Primordiais". */
    public static final DamageScope NON_ELEMENTAL_NON_PRIMORDIAL = new DamageScope(Set.of(DamageType.FISICO, DamageType.MAGICO), null);

    /** Danos of one element, physical or not. */
    public static DamageScope element(@NonNull final ElementalType element) {
        return new DamageScope(Set.of(DamageType.ELEMENTAL, DamageType.FISICO_ELEMENTAL), element);
    }

    /** Whether a hit of {@code type}, described by {@code descriptor} (nullable), falls inside this scope. */
    public boolean matches(final DamageType type, final DamageDescriptor descriptor) {
        DamageType effectiveType = descriptor != null ? descriptor.damageType() : type;
        if (effectiveType == null || !types.contains(effectiveType)) {
            return false;
        }
        if (element == null) {
            return true;
        }
        if (descriptor == null || descriptor.elementalType() == null) {
            return false;
        }
        return element == ElementalType.TODOS || element == descriptor.elementalType();
    }
}
