package org.aventyrs.core.character.services;

import org.aventyrs.core.ability.AttributeAbility;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.sheet.CombatantSheet;

public class LifeStealServiceImpl implements LifeStealService {

    @Override
    public int getTotalLifeSteal(final Character character, final CombatantSheet characterSheet) {
        int base = characterSheet.getTotalLifeSteal() + character.getFeats().stream()
                .mapToInt(feat -> feat.resolveGrantedLifeSteal(character))
                .sum();
        if (base <= 0) {
            return base;
        }
        int bonus = character.getAttributeAbilities().stream()
                .mapToInt(AttributeAbility::resolveLifeStealBonus)
                .sum();
        // The sheet-aware form, so a Forma-gated amplifier (Morcego Atroz's "+2") can see the shape
        // its holder is wearing. Defaults down to the Character-only form for every other Talento.
        bonus += character.getFeats().stream()
                .mapToInt(feat -> feat.resolveLifeStealBonus(character, characterSheet))
                .sum();
        return base + bonus;
    }
}
