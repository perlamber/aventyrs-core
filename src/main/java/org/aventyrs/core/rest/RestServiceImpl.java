package org.aventyrs.core.rest;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.sheet.CharacterSheet;

public class RestServiceImpl implements RestService {

    @Override
    public int getRecoveredHitPoints(final Character character, final RestType restType) {
        int bonus = character.getAttributeAbilities().stream()
                .mapToInt(ability -> ability.resolveRestHitPointsBonus(restType))
                .sum();
        return recovered(character.getAttributes().getVigor().getTotal(), restType) + bonus;
    }

    @Override
    public int getRecoveredMagicPoints(final Character character, final RestType restType) {
        int bonus = character.getAttributeAbilities().stream()
                .mapToInt(ability -> ability.resolveRestMagicPointsBonus(restType))
                .sum();
        return recovered(character.getAttributes().getFocus().getTotal(), restType) + bonus;
    }

    @Override
    public int getRecoveredDeterminationPoints(final Character character, final RestType restType) {
        int bonus = character.getAttributeAbilities().stream()
                .mapToInt(ability -> ability.resolveRestDeterminationPointsBonus(restType))
                .sum();
        return recovered(character.getAttributes().getInstinct().getTotal(), restType) + bonus;
    }

    @Override
    public void applyRest(final Character character, final CharacterSheet characterSheet, final RestType restType) {
        characterSheet.heal(getRecoveredHitPoints(character, restType));
        characterSheet.recoverMagicPoints(getRecoveredMagicPoints(character, restType));
        characterSheet.recoverDeterminationPoints(getRecoveredDeterminationPoints(character, restType));
        characterSheet.applyPendingEgoRecoveries(restType);
    }

    /**
     * Descanso Mínimo (x0.5) can yield a fractional result on an odd Attribute; rounded
     * down, as the ruleset does elsewhere when a half-value isn't specified further.
     */
    private int recovered(final int attributeTotal, final RestType restType) {
        return (int) Math.floor(attributeTotal * restType.getAttributeMultiplier());
    }
}
