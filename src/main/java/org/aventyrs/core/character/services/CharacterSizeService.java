package org.aventyrs.core.character.services;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.SizeCategory;

public interface CharacterSizeService {
    /**
     * The character's Size Category after every source has had its say, clamped by {@link
     * SizeCategory#shift}.
     *
     * <p>Two kinds of source, resolved in that order: a Talento that <b>sets</b> the category
     * outright ({@code Feat#resolveSizeCategoryOverride} — "Sua Categoria de Tamanho muda para
     * -2") replaces the character's own, and then every <b>shift</b> applies on top — the {@code
     * ModifierType#SIZE_CATEGORY} scan (the Sangue de Gigante Vigor ability) plus every Talento's
     * {@code Feat#resolveSizeCategoryIncrease}.
     */
    SizeCategory getEffectiveSizeCategory(Character character);
}
