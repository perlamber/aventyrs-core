package org.aventyrs.core.character.services;

import org.aventyrs.core.action.ActionProfile;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CombatantSheet;
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
     *
     * <p>The <em>permanent</em> total only, same as {@link ReactionsService#getTotalReactions(Character)}:
     * no turnNumber means no {@link ActionProfile} adjustment, and no sheet means no Round-scoped
     * {@code TemporaryBonus}. See {@link #getTotalFreeActions(CombatantSheet, int, SceneContext)}.
     */
    int getTotalFreeActions(Character character);

    /**
     * {@link #getTotalFreeActions(CombatantSheet, int, SceneContext)} with no Scene in hand —
     * delegates down with a {@code null} {@link SceneContext}.
     */
    int getTotalFreeActions(CombatantSheet sheet, int turnNumber);

    /**
     * How many Ações Livres this combatant has on the given Round: the permanent total of
     * {@link #getTotalFreeActions(Character)}, plus the sheet's own
     * {@link CombatantSheet#getTemporaryBonus(ModifierType)} for
     * {@link ModifierType#FREE_ACTIONS}, then the character's {@link ActionProfile} adjustment
     * for that Round applied last. turnNumber is 0-based. Never negative.
     *
     * <p>Applying the profile last is what makes {@link ActionProfile#MOVIMENTO_PLANEJADO}'s
     * first-Turn denial real: "não pode fazer Ações Livres" returns exactly 0 no matter how
     * many the character had accumulated, and only from the second Turn onward does its own
     * extra Ação Livre stack on top.
     */
    int getTotalFreeActions(CombatantSheet sheet, int turnNumber, SceneContext sceneContext);
}
