package org.aventyrs.core.character.services;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.skill.SkillExcellency;

public interface FreeActionsService {
    /** Every character starts with 1 Ação Livre before any Talento/Habilidade modifier applies. */
    int DEFAULT_FREE_ACTIONS = 1;

    /**
     * Total Ações Livres available: the character's own fixed counter plus any
     * {@link org.aventyrs.core.modifier.ModifierType#FREE_ACTIONS} modifier found on
     * attributeAbilities, skillCompetencyAbilities, or the unlocked
     * {@link org.aventyrs.core.skill.SkillExcellency} tiers of every trained Perícia. Never
     * negative.
     *
     * <p>Unlike {@link ReactionsService}, an Ação Livre can be spent on the character's own
     * Turn rather than only in response to someone else's — the two counters share the exact
     * same aggregation shape, they just differ in when they may be spent.
     */
    int getTotalFreeActions(Character character);
}
