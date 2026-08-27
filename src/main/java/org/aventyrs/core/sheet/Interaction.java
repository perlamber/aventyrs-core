package org.aventyrs.core.sheet;

/**
 * Something that can be performed on any {@link Interactable}, like a Perícia roll test,
 * using experience, spending points, etc. Each concrete Interaction owns the logic to
 * compute its own {@link InteractionResult} from the target's characteristics.
 */
public interface Interaction<R extends Interactable> {
    InteractionResult applyTo(R target );

    /**
     * The stage this Interaction hands off to once it's done, or {@code null} when it's the end
     * of the chain (the default — most Interactions are a single step).
     *
     * <p>This is the <i>pre-wired</i> half of the pipeline {@code org.aventyrs.core.effect}'s
     * package-info documents. That pipeline drives itself off {@link
     * InteractionResult#getNextInteraction()}, which a stage decides per-application; carrying
     * the successor on the Interaction <i>instance</i> instead lets a whole multi-stage chain be
     * assembled up front — {@code org.aventyrs.core.combat.AttackReceiver} builds one from a
     * DamageInteraction plus however many Efeitos Críticos and Correntes de Efeitos an attack
     * triggers — and handed over as one object. An implementation that carries a successor is
     * responsible for copying it onto its own result (see {@code
     * org.aventyrs.core.effect.AbstractEffect}), which is what keeps the existing
     * drain-the-result loop working unchanged.
     */
    default Interaction<R> getNextInteraction() {
        return null;
    }
}
