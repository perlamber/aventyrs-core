package org.aventyrs.core.character;

/**
 * How devoted a character is to their {@link Deity} — the three rungs every Talento de Devoção is split across
 * ("Adepto", "Fiel", "Fundamentalista"; {@code feat.DevotoFeat}). Table ruling (2026-10-01): the player picks it at
 * creation, and afterwards the Narrador raises or lowers it on role-play alone — nothing here prices, earns or
 * validates a tier ({@code DevotionService#setTier}). A Talento's rung applies while the holder is at least at it.
 */
public enum DevotionTier {

    ADEPTO,
    FIEL,
    FUNDAMENTALISTA;

    /** Whether this tier reaches rung — a Fundamentalista is also Fiel and Adepto. */
    public boolean reaches(final DevotionTier rung) {
        return rung != null && compareTo(rung) >= 0;
    }
}
