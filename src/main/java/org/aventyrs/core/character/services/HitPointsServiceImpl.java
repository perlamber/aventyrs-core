package org.aventyrs.core.character.services;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.feat.Feat;
import org.aventyrs.core.character.CharacterStatus;
import org.aventyrs.core.modifier.ModifierResolver;
import org.aventyrs.core.modifier.ModifierResolverImpl;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.skill.SkillCompetencyAbility;

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
        return getLifeMultiplier(character, null);
    }

    @Override
    public int getLifeMultiplier(final Character character, final CombatantSheet characterSheet) {
        int bonus = modifierResolver.sumModifiers(character.getAttributeAbilities(), ModifierType.LIFE_MULTIPLIER);
        // Talentos are outside every ModifierResolver scan, so they get an explicit pass — the
        // same shape MagicPointsServiceImpl uses for resolveManaMultiplierIncrease.
        for (Feat feat : character.getFeats()) {
            bonus += feat.resolveLifeMultiplierIncrease(character, characterSheet);
        }
        if (characterSheet != null) {
            // A held Malefício can lower it — ConditionType#ENVENENADO's "-1 Multiplicador de
            // Pontos de Vida". Only reachable with a sheet, like every other condition read; the
            // Character-only overload has nowhere to look. Floored at 1 so a stack of Malefícios
            // can never drive a creature's PV to zero by arithmetic alone.
            bonus += characterSheet.getConditionBonus(ModifierType.LIFE_MULTIPLIER, null);
            // And a held timed/combat-scoped loss — Ferida Profunda's "perde 3 Multiplicadores de PV".
            bonus += characterSheet.getTemporaryBonus(ModifierType.LIFE_MULTIPLIER);
        }
        return Math.max(1, character.getLifeMultiplier() + bonus);
    }

    @Override
    public int getHitPointsBonus(final Character character) {
        int bonus = modifierResolver.sumModifiers(character.getAttributeAbilities(), ModifierType.HIT_POINTS);
        return bonus + modifierResolver.sumModifiers(
                SkillCompetencyAbility.allFor(character), ModifierType.HIT_POINTS);
    }

    @Override
    public int getMaxHitPoints(final Character character) {
        return getMaxHitPoints(character, null);
    }

    @Override
    public int getMaxHitPoints(final Character character, final CombatantSheet characterSheet) {
        return BASE_HIT_POINTS
                + character.getEffectiveAttributeTotal(AttributeDomain.VIGOR, characterSheet)
                        * getLifeMultiplier(character, characterSheet)
                + getHitPointsBonus(character);
    }

    @Override
    public int getCurrentHitPoints(final Character character, final CombatantSheet characterSheet) {
        return Math.max(0, getMaxHitPoints(character, characterSheet) - characterSheet.getDamageTaken());
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

    @Override
    public CharacterStatus getStatus(final CombatantSheet characterSheet) {
        int maxHitPoints = getMaxHitPoints(characterSheet.getCharacter(), characterSheet);
        return getStatus(maxHitPoints - characterSheet.getDamageTaken(), maxHitPoints);
    }
}
