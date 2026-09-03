package org.aventyrs.core.sheet;

import lombok.Getter;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.scene.SceneContext;

/**
 * One combatant actually being under a {@link ConditionType} — the held instance to that enum's
 * catalogue entry, the same split {@code Item} draws against its own catalogue. A {@link
 * TemporaryEffect}, so it counts down in Rodadas, expires and is discarded by {@link
 * CombatantSheet#tickTemporaryEffects()} with no machinery of its own, exactly like {@link
 * Bleeding}/{@link ManaDrain}/{@link TemporaryBonus}.
 *
 * <p><b>Never cumulative.</b> Being Desprevenido twice is not worse than being Desprevenido once
 * — a condition is a state, not a stacking bonus — so {@link #isCumulative()} is {@code false}
 * and re-applying a condition replaces the held instance, refreshing its duration. That is
 * narrower than {@link TemporaryEffect}'s type-based default: two Conditions are the "same kind"
 * only when they name the same {@link ConditionType}, not merely when both are Conditions.
 *
 * <p>{@code source} is the combatant this condition <i>originates from</i> — "a origem de seu
 * medo" for the fear ladder, the captor for Agarrado. It is what the range-scoped halves of
 * {@link ConditionType#getEffects()}/{@link ConditionType#getImplied()} measure against, and is
 * {@code null} for a condition with no origin (Cego, Silêncio, Feridas Dolorosas), in which case
 * a range-scoped effect cannot be resolved and does not apply — see {@link
 * #appliesWithin(Range, SceneContext)}.
 */
@Getter
public class Condition extends TemporaryEffect {

    private final ConditionType type;
    private final CombatantSheet source;

    /** A condition with no origin — Cego, Silêncio, Feridas Dolorosas. */
    public Condition(final ConditionType type, final Integer remainingRounds) {
        this(type, remainingRounds, null);
    }

    public Condition(final ConditionType type, final Integer remainingRounds, final CombatantSheet source) {
        super(remainingRounds);
        this.type = type;
        this.source = source;
    }

    /**
     * False, always — see the class javadoc. {@link CombatantSheet#applyEffect} uses this to
     * decide whether to replace, but compares by {@code getClass()}, which would make every
     * Condition replace every other; {@code AbstractCombatantSheet#applyCondition} narrows that
     * to same-{@link ConditionType} replacement instead.
     */
    @Override
    boolean isCumulative() {
        return false;
    }

    /**
     * The value this condition contributes toward modifierType right now, given sceneContext —
     * summing every {@link ConditionType.ConditionEffect} of that type whose own proximity scope
     * is currently satisfied. 0 when none is.
     */
    public int resolveBonus(final ModifierType modifierType, final SceneContext sceneContext) {
        return type.getEffects().stream()
                .filter(effect -> effect.type() == modifierType)
                .filter(effect -> appliesWithin(effect.within(), sceneContext))
                .mapToInt(ConditionType.ConditionEffect::value)
                .sum();
    }

    /**
     * Whether a scope of {@code within} is currently satisfied — trivially true for {@code null}
     * (an unscoped effect), otherwise whether this condition's {@link #getSource()} is at that
     * {@link Range} or closer.
     *
     * <p>Returns {@code false} rather than defaulting to "applies" whenever the distance cannot
     * be established — no {@code source}, no {@code sceneContext}, or a source the context has no
     * distance for. A proximity-scoped malus is stated as conditional, so applying it with no way
     * to test the condition would over-punish; the same reading every {@code SceneContext}-gated
     * ability in this core takes of a {@code null} context.
     */
    public boolean appliesWithin(final Range within, final SceneContext sceneContext) {
        if (within == null) {
            return true;
        }
        if (source == null || sceneContext == null) {
            return false;
        }
        Range distance = sceneContext.getDistanceTo(source);
        return distance != null && distance.isWithin(within);
    }
}
