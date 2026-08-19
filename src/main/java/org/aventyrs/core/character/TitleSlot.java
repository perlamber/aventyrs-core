package org.aventyrs.core.character;

/**
 * Which of a Character's three Título Aventyr slots a held {@link
 * org.aventyrs.core.title.AventyrTitle} occupies — mirrors {@link EgoDomain}'s own shape (the
 * key type lives here, alongside {@link Character}, while the value type — {@code
 * AventyrTitle} — lives in its own {@code org.aventyrs.core.title} package). Structural
 * position, not a self-reported flag on the held Título itself, is what makes a Título
 * "Primário" — see {@link Character#getPrimaryTitle()}.
 */
public enum TitleSlot {
    PRIMARY,
    SECONDARY,
    TERTIARY
}
