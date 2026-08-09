package org.aventyrs.core.character.services;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.skill.SkillExcellency;

public interface InitiativeService {

    /**
     * Total Iniciativa: the character's Iniciativa Ego total ({@code character.getEgos()
     * .getIniciativa().getTotal()}) plus any {@link org.aventyrs.core.modifier.ModifierType#INITIATIVE}
     * modifier found on attributeAbilities, skillCompetencyAbilities, or the unlocked
     * {@link SkillExcellency} tiers of every trained Perícia — the same three-source
     * aggregation {@link ReactionsService}/{@link FreeActionsService} use.
     *
     * <p>Unlike those two counters, this isn't a spendable resource, so it's never floored at
     * 0 — a large enough malus can genuinely leave a character with a negative total, still a
     * valid (if late) position in turn order.
     *
     * <p>This is only the fixed component: the actual value used to sort a
     * {@link org.aventyrs.core.scene.Scene}'s turn order also adds the dice roll the caller
     * applied, which this core doesn't compute itself — see
     * {@link org.aventyrs.core.scene.InitiativeEntry}.
     */
    int getTotalInitiative(Character character);
}
