package org.aventyrs.core.character.services;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.modifier.ModifierResolver;
import org.aventyrs.core.modifier.ModifierResolverImpl;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.sheet.CharacterSheet;

public class DeterminationPointsServiceImpl implements DeterminationPointsService {

    private final ModifierResolver modifierResolver;

    public DeterminationPointsServiceImpl() {
        this(new ModifierResolverImpl());
    }

    public DeterminationPointsServiceImpl(final ModifierResolver modifierResolver) {
        this.modifierResolver = modifierResolver;
    }

    @Override
    public int getDeterminationMultiplier(final Character character) {
        int bonus = modifierResolver.sumModifiers(character.getAttributeAbilities(), ModifierType.DETERMINATION_MULTIPLIER);
        return DEFAULT_DETERMINATION_MULTIPLIER + bonus;
    }

    @Override
    public int getMaxDeterminationPoints(final Character character) {
        return character.getAttributes().getInstinct().getTotal() * getDeterminationMultiplier(character);
    }

    @Override
    public int getCurrentDeterminationPoints(final Character character, final CharacterSheet characterSheet) {
        return Math.max(0, getMaxDeterminationPoints(character) - characterSheet.getDeterminationSpent());
    }
}
