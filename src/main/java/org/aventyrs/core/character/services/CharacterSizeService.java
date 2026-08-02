package org.aventyrs.core.character.services;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.SizeCategory;

public interface CharacterSizeService {
    /**
     * The character's Size Category after applying bonuses from sources such as the
     * Sangue de Gigante Vigor ability, clamped to the -4/+4 range.
     */
    SizeCategory getEffectiveSizeCategory(Character character);
}
