package org.aventyrs.core.sheet;

import org.aventyrs.core.character.DamageDescriptor;
import org.aventyrs.core.character.DamageType;

/**
 * What a harmful continuous effect is, for the resistances that shorten its Duração (acoes-de-combate.txt, 20261013):
 * RD "Efeitos Físicos contínuos não-PRIMORDIAIS que inflija danos", RM "Efeitos Mágicos contínuos não-PRIMORDIAIS", RE
 * "Efeitos Elementais contínuos", RA "Efeitos contínuos". Declared by the effect itself ({@link
 * TemporaryEffect#getContinuousNature()}); an effect declaring none is never shortened.
 *
 * @param descriptor its damage type and element/sanctity; a {@code null} one is harmful but of no stated nature, so
 *                   only RA reaches it
 * @param damaging   whether it inflicts damage — RD reaches only one that does
 */
public record ContinuousNature(DamageDescriptor descriptor, boolean damaging) {

    /** A damaging effect of this descriptor. */
    public static ContinuousNature damaging(final DamageDescriptor descriptor) {
        return new ContinuousNature(descriptor, true);
    }

    /** A damaging effect of a plain type. */
    public static ContinuousNature damaging(final DamageType type) {
        return damaging(new DamageDescriptor(type));
    }

    /** A harmful effect with no stated nature — a drain. */
    public static final ContinuousNature UNTYPED = new ContinuousNature(null, false);

    /** The effect's damage type, or {@code null}. */
    public DamageType type() {
        return descriptor == null ? null : descriptor.damageType();
    }
}
