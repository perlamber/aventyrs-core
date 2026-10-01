package org.aventyrs.core.feat;

import lombok.NonNull;
import org.aventyrs.core.character.DevotionTier;

/**
 * One choice a Talento de Devoção's rung asks for — Astúcia de Sylph's Habilidade de Competência de Atenção at
 * Adepto, Impacto Ymiriano's Força or Vigor at Fundamentalista. Made when the holder first reaches that rung (at
 * creation for the starting tier) and <b>kept</b> on the character ({@code Character#getDevotionPicks()}): a tier the
 * Narrador lowers silences the rung, and raising it back brings the same pick back (table ruling, 2026-10-01).
 *
 * @param talento the Talento de Devoção the pick belongs to
 * @param rung    the rung that asked for it
 * @param value   what was picked — a {@code SkillTrait}, an {@code AttributeDomain}, or the Talento's own option enum
 */
public record DevotionPick(@NonNull DevotoFeat talento, @NonNull DevotionTier rung, @NonNull Object value) {
}
