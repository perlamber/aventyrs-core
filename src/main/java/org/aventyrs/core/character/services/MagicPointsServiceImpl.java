package org.aventyrs.core.character.services;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.modifier.ModifierResolver;
import org.aventyrs.core.modifier.ModifierResolverImpl;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.sheet.CombatantSheet;

public class MagicPointsServiceImpl implements MagicPointsService {

    private final ModifierResolver modifierResolver;

    public MagicPointsServiceImpl() {
        this(new ModifierResolverImpl());
    }

    public MagicPointsServiceImpl(final ModifierResolver modifierResolver) {
        this.modifierResolver = modifierResolver;
    }

    @Override
    public int getManaMultiplier(final Character character) {
        int bonus = modifierResolver.sumModifiers(character.getAttributeAbilities(), ModifierType.MANA_MULTIPLIER);
        return character.getManaMultiplier() + bonus;
    }

    @Override
    public int getMaxMagicPoints(final Character character) {
        return BASE_MAGIC_POINTS + character.getAttributes().getFocus().getTotal() * getManaMultiplier(character);
    }

    @Override
    public int getCurrentMagicPoints(final Character character, final CombatantSheet characterSheet) {
        return Math.max(0, getMaxMagicPoints(character) - characterSheet.getManaSpent());
    }
}
