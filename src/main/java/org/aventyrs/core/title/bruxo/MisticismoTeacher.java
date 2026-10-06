package org.aventyrs.core.title.bruxo;

import org.aventyrs.core.title.AventyrTitleAbility;

import java.util.Optional;

/**
 * A Bruxo trait whose rules text says "Você aprende um Misticismo". Nearly every one does, so every
 * Bruxo catalog enum implements this. The trait's {@code name()} is the key {@link Bruxo} stores
 * its pick under, which is what a consumer persists.
 */
public interface MisticismoTeacher extends AventyrTitleAbility {

    /** Which Árvores this trait's clause admits, or empty when it teaches no Misticismo. */
    Optional<MisticismoFilter> getMisticismoFilter();

    /** The enum constant's own name — the persistence key of the pick it carries. */
    String name();
}
