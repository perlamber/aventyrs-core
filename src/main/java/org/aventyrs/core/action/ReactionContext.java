package org.aventyrs.core.action;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.skill.SkillType;

/**
 * Everything the question "what can this combatant react with right now" needs, bundled — the
 * request half of {@code ReactionOptionsService#getAvailableReactions}, in the same "one required
 * field plus optional ones" shape as {@code TitleAbilityActivationRequest} and {@code
 * SkillRollRequest}. Each ability reads only what its own rules text needs, through {@code
 * AventyrTitleAbility#isReactionAvailable}.
 */
@Getter
@Builder
public class ReactionContext {

    /** What just happened. */
    @NonNull
    private final ReactionTrigger trigger;

    /** Who might react to it. */
    @NonNull
    private final CombatantSheet reactor;

    /**
     * The <b>reactor's own</b> snapshot of nearby allies/enemies — the correct one here, since
     * every distance a Reação reads is measured from the reactor. {@code null} outside an
     * encounter, which yields no reactions at all rather than all of them.
     */
    private final SceneContext reactorContext;

    /**
     * The ally the trigger happened to, for a trigger that names one ({@link
     * ReactionTrigger#ALLY_TARGETED_BY_ATTACK}). {@code null} otherwise.
     */
    private final CombatantSheet threatenedAlly;

    /** Who caused the trigger, when that is a combatant — the attacker. {@code null} otherwise. */
    private final CombatantSheet attacker;

    /** The Perícia of the attack behind the trigger, when there is one. {@code null} otherwise. */
    private final SkillType attackSkill;

    /**
     * Whether the attack behind the trigger names the reactor as its <b>only</b> target — false
     * for a multi-target attack. {@code true} by default, since an ordinary attack has one.
     */
    @Builder.Default
    private final boolean soleTarget = true;

    /** The Rodada this is happening in, for the counters that vary by Turn. */
    private final int turnNumber;

    /**
     * The already-mitigated damage about to land, for {@link ReactionTrigger#SELF_WOULD_DROP_TO_ZERO_HP};
     * {@code null} for every other trigger.
     */
    private final Integer pendingDamage;
}
