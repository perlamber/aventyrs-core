package org.aventyrs.core.character.services;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.sheet.CombatantSheet;

public interface LifeStealService {
    /**
     * Total Roubo de Vida character currently has: the sum of every currently-active {@code
     * LifeSteal} effect's own value ({@link CombatantSheet#getTotalLifeSteal()}) and standing
     * Talento grants ({@link org.aventyrs.core.feat.Feat#resolveGrantedLifeSteal}), plus whichever {@link
     * org.aventyrs.core.ability.AttributeAbility#resolveLifeStealBonus} bonuses apply on top
     * of it (e.g. {@code VigorAbility#METABOLISMO_RAPIDO}'s own +1) — but only once a source
     * is already positive. The bonus hooks only amplify an existing source; they never grant
     * one from nothing.
     */
    int getTotalLifeSteal(Character character, CombatantSheet characterSheet);
}
