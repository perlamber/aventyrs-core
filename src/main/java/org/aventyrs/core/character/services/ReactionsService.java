package org.aventyrs.core.character.services;

import org.aventyrs.core.action.ActionProfile;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CombatantSheet;
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
     *
     * <p>The <em>permanent</em> total only. It takes no turnNumber, so it can't apply the
     * character's {@link ActionProfile} — and no sheet, so it can't see a Round-scoped
     * {@code TemporaryBonus}. See {@link #getTotalReactions(CombatantSheet, int, SceneContext)}
     * for what a combatant actually has on a given Round.
     */
    int getTotalReactions(Character character);

    /**
     * {@link #getTotalReactions(CombatantSheet, int, SceneContext)} with no Scene in hand —
     * delegates down with a {@code null} {@link SceneContext}.
     */
    int getTotalReactions(CombatantSheet sheet, int turnNumber);

    /**
     * How many Reações this combatant has on the given Round: the permanent total of
     * {@link #getTotalReactions(Character)}, plus the sheet's own
     * {@link CombatantSheet#getTemporaryBonus(ModifierType)} for {@link ModifierType#REACTIONS},
     * then the character's {@link ActionProfile} adjustment for that Round applied last (see
     * {@link ActionProfile#REFLEXOS_RAPIDOS}, {@link ActionProfile#ESTRATEGISTA}). turnNumber is
     * 0-based. Never negative.
     *
     * <p>A {@code null} sceneContext reads as "not a Cena de Combate", which is what a caller
     * resolving Reações outside a Scene wants.
     */
    int getTotalReactions(CombatantSheet sheet, int turnNumber, SceneContext sceneContext);
}
