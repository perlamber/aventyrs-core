package org.aventyrs.core.character.services;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.skill.SkillExcellency;

public interface ReactionsService {
    /** Every character starts with 1 Reação before any Talento/Habilidade modifier applies. */
    int DEFAULT_REACTIONS = 1;

    /**
     * Total Reações available: the character's own fixed counter plus any
     * {@link org.aventyrs.core.modifier.ModifierType#REACTIONS} modifier found on
     * attributeAbilities, skillCompetencyAbilities, or the unlocked
     * {@link org.aventyrs.core.skill.SkillExcellency} tiers of every trained Perícia. Never
     * negative.
     */
    int getTotalReactions(Character character);
}
