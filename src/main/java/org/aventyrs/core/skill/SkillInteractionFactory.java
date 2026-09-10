package org.aventyrs.core.skill;

import org.aventyrs.core.sheet.InteractionResult;

/**
 * Resolves which concrete {@code <Skill>Interaction} to use for a given {@link SkillType},
 * so a caller (e.g. an API layer dispatching an incoming roll request) never has to
 * hand-write a switch over all 14 concrete classes — {@link SkillType} already carries the
 * mapping (see {@link SkillType#newInteraction()}), this is just the single entry point for
 * using it.
 */
public final class SkillInteractionFactory {

    private SkillInteractionFactory() {
    }

    /** A fresh {@code <Skill>Interaction} for skillType, built via its default constructor. */
    public static AbstractSkillInteraction create(final SkillType skillType) {
        return skillType.newInteraction();
    }

    /**
     * Resolves request's {@link SkillType} to the right {@code <Skill>Interaction} and applies
     * it to request's target/sceneContext/skillRoll/attackSource in one call — the single entry
     * point a caller wrapping up an incoming roll request (see {@link SkillRollRequest}) needs,
     * instead of manually looking up the right {@code <Skill>Interaction} class itself.
     *
     * <p>{@code attackTarget} is passed as {@code null}: {@link SkillRollRequest} carries no
     * such field, and an attack that has a real defender goes through {@code
     * org.aventyrs.core.combat.AttackDelivery} instead, which supplies both it and the
     * attackSource.
     */
    public static InteractionResult resolve(final SkillRollRequest request) {
        return create(request.getSkillType())
                .applyTo(request.getTarget(), request.getSceneContext(), request.getSkillRoll(), null, request.getAttackSource());
    }
}
