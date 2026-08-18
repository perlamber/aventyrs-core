package org.aventyrs.core.character.services;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.sheet.CharacterSheet;

public interface LifeStealService {
    /**
     * Total Roubo de Vida character currently has active on characterSheet: the sum of every
     * currently-active {@code LifeSteal} effect's own value ({@link
     * CharacterSheet#getTotalLifeSteal()}), plus whichever {@link
     * org.aventyrs.core.ability.AttributeAbility#resolveLifeStealBonus} bonuses apply on top
     * of it (e.g. {@code VigorAbility#METABOLISMO_RAPIDO}'s own +1) — but only once that sum
     * is already positive. A character with no active {@code LifeSteal} effect of their own
     * gets none of those bonuses either, since they only ever amplify an already-active
     * effect, never grant one from nothing.
     */
    int getTotalLifeSteal(Character character, CharacterSheet characterSheet);
}
