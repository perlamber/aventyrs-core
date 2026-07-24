package org.aventyrs.core.character.services;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.SizeCategory;
import org.aventyrs.core.modifier.ModifierResolver;
import org.aventyrs.core.modifier.ModifierResolverImpl;
import org.aventyrs.core.modifier.ModifierType;

public class CharacterSizeServiceImpl implements CharacterSizeService {

    private final ModifierResolver modifierResolver;

    public CharacterSizeServiceImpl() {
        this(new ModifierResolverImpl());
    }

    public CharacterSizeServiceImpl(final ModifierResolver modifierResolver) {
        this.modifierResolver = modifierResolver;
    }

    @Override
    public SizeCategory getEffectiveSizeCategory(final Character character) {
        int bonus = modifierResolver.sumModifiers(character.getAttributeAbilities(), ModifierType.SIZE_CATEGORY);
        return character.getSizeCategory().shift(bonus);
    }
}
