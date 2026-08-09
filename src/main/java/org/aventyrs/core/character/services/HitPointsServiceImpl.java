package org.aventyrs.core.character.services;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterStatus;
import org.aventyrs.core.modifier.ModifierResolver;
import org.aventyrs.core.modifier.ModifierResolverImpl;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.sheet.CharacterSheet;

public class HitPointsServiceImpl implements HitPointsService {

    private final ModifierResolver modifierResolver;

    public HitPointsServiceImpl() {
        this(new ModifierResolverImpl());
    }

    public HitPointsServiceImpl(final ModifierResolver modifierResolver) {
        this.modifierResolver = modifierResolver;
    }

    @Override
    public int getLifeMultiplier(final Character character) {
        int bonus = modifierResolver.sumModifiers(character.getAttributeAbilities(), ModifierType.LIFE_MULTIPLIER);
        return DEFAULT_LIFE_MULTIPLIER + bonus;
    }

    @Override
    public int getMaxHitPoints(final Character character) {
        return BASE_HIT_POINTS + character.getAttributes().getVigor().getTotal() * getLifeMultiplier(character);
    }

    @Override
    public int getCurrentHitPoints(final Character character, final CharacterSheet characterSheet) {
        return Math.max(0, getMaxHitPoints(character) - characterSheet.getDamageTaken());
    }

    @Override
    public CharacterStatus getStatus(final int currentHitPoints, final int maxHitPoints) {
        if (currentHitPoints >= maxHitPoints) {
            return CharacterStatus.CLEAN;
        }
        if (currentHitPoints * 3 > maxHitPoints * 2) {
            return CharacterStatus.HIGH_LIFE;
        }
        if (currentHitPoints * 3 > maxHitPoints) {
            return CharacterStatus.MEDIUM_LIFE;
        }
        if (currentHitPoints > 0) {
            return CharacterStatus.LOW_LIFE;
        }
        if (currentHitPoints * 2 > -maxHitPoints) {
            return CharacterStatus.FALLEN;
        }
        if (currentHitPoints > -maxHitPoints) {
            return CharacterStatus.COMMA;
        }
        return CharacterStatus.DEAD;
    }
}
