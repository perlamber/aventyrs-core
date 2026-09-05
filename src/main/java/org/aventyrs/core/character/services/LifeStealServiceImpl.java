package org.aventyrs.core.character.services;

import org.aventyrs.core.ability.AttributeAbility;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.sheet.CombatantSheet;

public class LifeStealServiceImpl implements LifeStealService {

    @Override
    public int getTotalLifeSteal(final Character character, final CombatantSheet characterSheet) {
        int base = characterSheet.getTotalLifeSteal();
        if (base <= 0) {
            return base;
        }
        int bonus = character.getAttributeAbilities().stream()
                .mapToInt(AttributeAbility::resolveLifeStealBonus)
                .sum();
        bonus += character.getFeats().stream()
                .mapToInt(feat -> feat.resolveLifeStealBonus(character))
                .sum();
        return base + bonus;
    }
}
