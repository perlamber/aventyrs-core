package org.aventyrs.core.magic;

import lombok.NonNull;

/**
 * A Magia cast and held back to be released later — "Você pode conjurar e 'guardar' uma Magia …
 * para soltá-la como uma Ação Livre posteriormente" ({@code MetamagicoFeat#ARMAZENAR_MAGIA}). The
 * cast has already happened: {@link #cast} is the {@link SpellCastingResult} it produced, costs and
 * all, and releasing it hands that result back to the caller to apply. See {@link
 * SpellStorageService}.
 */
public record StoredSpell(@NonNull Spell spell, @NonNull SpellCastingResult cast) {
}
