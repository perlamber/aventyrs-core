package org.aventyrs.core.character.services;

import org.aventyrs.core.action.Manoeuvre;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.skill.SkillInteractionFactory;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.skill.SkillType;

/**
 * A manoeuvre's own attack roll, resolved without {@code AttackDelivery} because the manoeuvre deals no damage of its
 * own (Encontrão) or aims at an item rather than a Defesa (Atacar Objetos) — the shape {@code GrappleServiceImpl}
 * rolls Agarrar with.
 */
final class ManoeuvreRolls {

    private ManoeuvreRolls() {
    }

    /** roller's roll with skill as manoeuvre at cost against opponent; the total includes every bonus. */
    static Rolled roll(final CombatantSheet roller, final SkillType skill, final SkillRoll roll,
                       final Manoeuvre manoeuvre, final ActionCost cost, final SceneContext sceneContext,
                       final CombatantSheet opponent) {
        SkillRoll named = manoeuvre == null ? roll : roll.asManoeuvre(manoeuvre, cost);
        InteractionResult result = SkillInteractionFactory.create(skill)
                .applyTo(roller, sceneContext, named, skill.isAttackSkill() ? opponent : null, null);
        return new Rolled(result, result.getSkillRollBonus() + named.getTotal()
                + (opponent == null ? 0 : opponent.getAttackerAttackRollBonus(sceneContext)));
    }

    /** A resolved roll and its total. A tie reaches. */
    record Rolled(InteractionResult result, int total) {
        boolean blindFailed() {
            return Boolean.TRUE.equals(result.getBlindCheckFailed());
        }

        boolean criticalSuccess() {
            return result.getCriticalResult() != null && result.getCriticalResult().isCriticalSuccess();
        }

        boolean reaches(final int required) {
            return !blindFailed() && total >= required;
        }
    }
}
