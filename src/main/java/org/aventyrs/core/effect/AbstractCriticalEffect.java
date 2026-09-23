package org.aventyrs.core.effect;

import lombok.Getter;
import lombok.NonNull;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.InteractionResult;

/**
 * The shared shape of an Efeito Crítico built from its catalog entry by {@link CriticalEffects}:
 * it carries its {@link CriticalEffectContext} (severity, attacker, weapon, dice) and describes
 * itself off {@link CriticalEffectType}'s tier text. A subclass writes only {@link #resolve}.
 */
@Getter
public abstract class AbstractCriticalEffect extends AbstractEffect implements CriticalEffect {

    private final CriticalEffectContext context;

    protected AbstractCriticalEffect(@NonNull final CriticalEffectContext context) {
        this.context = context;
    }

    /** The severity-specific rules text, verbatim from {@code docs/rules/efeitos-criticos.txt}. */
    protected abstract String majorDescription();

    protected abstract String minorDescription();

    @Override
    public String getDescription() {
        return context.isMajor() ? majorDescription() : minorDescription();
    }

    @Override
    public final InteractionResult applyTo(final CombatantSheet target) {
        InteractionResult.InteractionResultBuilder result = resolve(target);
        return reportChain(result.resultStatus(resolveStatus(target))).build();
    }

    /** The effect itself on target; the chain forwarding and the status are added after. */
    protected abstract InteractionResult.InteractionResultBuilder resolve(CombatantSheet target);

    /** The attacker, or {@code null}. */
    protected CombatantSheet attacker() {
        return context.attacker();
    }

    /** The Maior figure or the Menor one. */
    protected int pick(final int major, final int minor) {
        return context.pick(major, minor);
    }

    /** count d6 from the context's roller — callers only build a dice-bearing effect with one. */
    protected int roll(final int count) {
        return context.dice().rollD6(count);
    }
}
