package org.aventyrs.core.modifier;

import java.util.Collection;

public interface ModifierResolver {
    /**
     * Sums every {@link Modifier} of the given type found on the source object's methods.
     */
    int sumModifiers(Object source, ModifierType type);

    /**
     * Sums every {@link Modifier} of the given type found across all source objects —
     * abilities, feats, titles, items, or any future bonus-granting concept.
     */
    int sumModifiers(Collection<?> sources, ModifierType type);
}
