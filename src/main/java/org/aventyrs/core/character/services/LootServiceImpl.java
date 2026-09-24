package org.aventyrs.core.character.services;

import org.aventyrs.core.character.CharacterStatus;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.IllegalOperationException;

import static org.aventyrs.core.util.TranslatableMessages.LOOT_TARGET_NOT_AN_ENEMY;
import static org.aventyrs.core.util.TranslatableMessages.LOOT_TARGET_NOT_DEFEATED;

public class LootServiceImpl implements LootService {

    @Override
    public ActionCost getLootCost(final SceneContext context) {
        return context != null && context.isCombatScene() ? LOOT_COST : ActionCost.NONE;
    }

    @Override
    public boolean canLoot(final SceneContext looterContext, final CombatantSheet target,
                           final CharacterStatus targetStatus) {
        return refusalFor(looterContext, target, targetStatus) == null;
    }

    @Override
    public ActionCost requireLootable(final SceneContext looterContext, final CombatantSheet target,
                                      final CharacterStatus targetStatus) throws IllegalOperationException {
        String refusal = refusalFor(looterContext, target, targetStatus);
        if (refusal != null) {
            throw new IllegalOperationException(refusal);
        }
        return getLootCost(looterContext);
    }

    /**
     * The message key for why this Saquear would be refused, or {@code null} when it is allowed,
     * so that {@link #canLoot} and {@link #requireLootable} always agree. Enemies are matched by
     * id, because the context may hold a different stand-in instance for the same combatant.
     */
    private static String refusalFor(final SceneContext looterContext, final CombatantSheet target,
                                     final CharacterStatus targetStatus) {
        boolean enemy = target != null && looterContext != null && looterContext.getEnemies().stream()
                .anyMatch(sheet -> sheet.getId().equals(target.getId()));
        if (!enemy) {
            return LOOT_TARGET_NOT_AN_ENEMY;
        }
        return LootService.isDefeated(targetStatus) ? null : LOOT_TARGET_NOT_DEFEATED;
    }
}
