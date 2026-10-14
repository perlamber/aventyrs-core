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
 * @param types    the {@link DamageType}s it reaches
 * @param element  the element it is limited to, or {@code null} for any; {@link ElementalType#TODOS}
 *                 matches every element
 * @param sanctity the sacred or profane nature it is limited to, or {@code null} for any (core 0.0.89) — a hit only
 *                 matches one with a {@link DamageDescriptor} naming that nature
 * @param elementFree whether a hit carrying any element is outside it — "que não sejam Elementais", which since the
 *                 2026-10-12 taxonomy must also leave out a Mágico Elemental
 */
public record DamageScope(@NonNull Set<DamageType> types, ElementalType element, DamageSanctity sanctity,
                          boolean elementFree) {

    /** A scope of no particular nature — every scope before core 0.0.89. */
    public DamageScope(@NonNull final Set<DamageType> types, final ElementalType element) {
        this(types, element, null, false);
    }

    public DamageScope(@NonNull final Set<DamageType> types, final ElementalType element, final DamageSanctity sanctity) {
        this(types, element, sanctity, false);
    }

    /** Every hit of one nature, whatever its type — "imunes a danos Profanos". */
    public static DamageScope sanctity(@NonNull final DamageSanctity sanctity) {
        return new DamageScope(Set.of(DamageType.values()), null, sanctity);
    }

    /** Every hit. */
    public static final DamageScope ALL = new DamageScope(Set.of(DamageType.values()), null);

    /**
     * "Danos Físicos" — a Físico hit, whatever its sanctity. Since the 2026-10-12 taxonomy an Elemental hit (the pre-update
     * Físico-Elemental) is no longer Físico.
     */
    public static final DamageScope PHYSICAL = new DamageScope(Set.of(DamageType.FISICO), null);

    /** "Danos Mágicos" — any Mágico hit, Elemental, Profano, Sagrado or Umbral. */
    public static final DamageScope MAGICAL = new DamageScope(Set.of(DamageType.MAGICO), null);

    /** "Efeitos e danos que não sejam Elementais ou Primordiais" — Físico, or a Mágico hit naming no element. */
    public static final DamageScope NON_ELEMENTAL_NON_PRIMORDIAL =
            new DamageScope(Set.of(DamageType.FISICO, DamageType.MAGICO), null, null, true);

    /** Danos of one element — Elemental or Mágico Elemental. */
    public static DamageScope element(@NonNull final ElementalType element) {
        return new DamageScope(Set.of(DamageType.MAGICO, DamageType.ELEMENTAL), element);
    }

    /** Whether a hit of {@code type}, described by {@code descriptor} (nullable), falls inside this scope. */
    public boolean matches(final DamageType type, final DamageDescriptor descriptor) {
        DamageType effectiveType = descriptor != null ? descriptor.damageType() : type;
        if (effectiveType == null || !types.contains(effectiveType)) {
            return false;
        }
        if (sanctity != null && (descriptor == null || descriptor.sanctity() != sanctity)) {
            return false;
        }
        if (elementFree && descriptor != null && descriptor.elementalType() != null) {
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
