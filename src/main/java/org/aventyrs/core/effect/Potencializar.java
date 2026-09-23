package org.aventyrs.core.effect;

import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.InteractionResult;

/**
 * Potencializar — "A duração da magia aumenta em +2d6 / +1d6 unidades". The figure is thrown at
 * construction and read through {@link #getDurationIncrease()}.
 *
 * <p>TODO the application: nothing tracks a Magia's running Duração on its target — {@code
 * SpellCastingService} reports a cast and forgets it — so there is no countdown to extend. The
 * caller that holds the cast's Duração adds this figure in the Magia's own unit.
 */
public class Potencializar extends AbstractCriticalEffect {

    private final int durationIncrease;


    public Potencializar(final CriticalEffectContext context) {
        super(context);
        this.durationIncrease = context.dice().rollD6(context.pick(2, 1));
    }

    @Override
    public CriticalEffectType getType() {
        return CriticalEffectType.POTENCIALIZAR;
    }

    @Override
    protected String majorDescription() {
        return "A duração da magia aumenta em +2d6 unidades.";
    }

    @Override
    protected String minorDescription() {
        return "A duração da magia aumenta em +1d6 unidades.";
    }

    @Override
    protected InteractionResult.InteractionResultBuilder resolve(final CombatantSheet target) {
        return InteractionResult.builder();
    }

    /** The units the Magia's Duração grows by — thrown once, when the effect was built. */
    public int getDurationIncrease() {
        return durationIncrease;
    }
}
