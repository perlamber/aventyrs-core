package org.aventyrs.core.character.services;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.feat.Feat;
import org.aventyrs.core.modifier.ModifierResolver;
import org.aventyrs.core.modifier.ModifierResolverImpl;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.sheet.CombatantSheet;

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
        // Talentos are outside every ModifierResolver scan, so they get an explicit pass — the
        // same shape MagicPointsServiceImpl uses for resolveManaMultiplierIncrease.
        for (Feat feat : character.getFeats()) {
            bonus += feat.resolveDeterminationMultiplierIncrease(character);
        }
        return character.getDeterminationMultiplier() + bonus;
    }

    @Override
    public int getMaxDeterminationPoints(final Character character) {
        return BASE_DETERMINATION_POINTS + character.getAttributes().getInstinct().getTotal() * getDeterminationMultiplier(character);
    }

    @Override
    public int getCurrentDeterminationPoints(final Character character, final CombatantSheet characterSheet) {
        return Math.max(0, getMaxDeterminationPoints(character) - characterSheet.getDeterminationSpent());
    }
}
