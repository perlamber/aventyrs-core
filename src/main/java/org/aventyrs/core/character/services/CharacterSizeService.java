package org.aventyrs.core.character.services;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.SizeCategory;
import org.aventyrs.core.sheet.CombatantSheet;

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

    /**
     * The same resolution, plus whatever round-scoped {@link
     * org.aventyrs.core.modifier.ModifierType#SIZE_CATEGORY} bonus sheet is currently holding —
     * a Forma's "sua Categoria de Tamanho aumenta em +2" ({@code DraconicoFeat#DRACONATO}).
     * Applied as one more shift, on top of the override, exactly like the other two shift sources.
     *
     * <p>The {@code Character}-only overload structurally cannot see it — there is no sheet to
     * ask — which is the same split the aggregate Pontos de Ação / Reações / RD reads already
     * carry. Every caller holding a sheet should prefer this one.
     */
    SizeCategory getEffectiveSizeCategory(CombatantSheet sheet);
}
