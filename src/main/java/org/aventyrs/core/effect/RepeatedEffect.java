package org.aventyrs.core.effect;

import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.Interaction;
import org.aventyrs.core.sheet.InteractionResult;

/**
 * One more application of an {@link Effect} already in a chain — what an attack whose Correntes de
 * Efeitos and Efeitos Críticos "aplicam seus efeitos duas vezes" ({@code ArtilhariaFeat#TIRO_DUPLO})
 * appends behind the original.
 *
 * <p>A wrapper rather than the same object placed twice: {@link AbstractEffect#chainInto} gives an
 * Effect exactly one successor, so the same instance at two points of a line would loop. This stage
 * runs its inner Effect and then hands on to <em>its own</em> successor, whatever the inner one's is.
 * It is the inner Effect that acts, so its own rules — a non-cumulative Condição replacing itself,
 * a dice-bearing effect reusing the dice it was built with — decide what a second application adds.
 */
public final class RepeatedEffect implements Effect {

    private final Effect repeated;
    private Interaction<CombatantSheet> nextInteraction;

    public RepeatedEffect(final Effect repeated) {
        this.repeated = repeated;
    }

    /** The Effect this stage applies again. */
    public Effect getRepeated() {
        return repeated;
    }

    @Override
    public String getDescription() {
        return repeated.getDescription();
    }

    @Override
    public Effect chainInto(final Interaction<CombatantSheet> nextInteraction) {
        this.nextInteraction = nextInteraction;
        return this;
    }

    @Override
    public Interaction<CombatantSheet> getNextInteraction() {
        return nextInteraction;
    }

    @Override
    public InteractionResult applyTo(final CombatantSheet target) {
        return repeated.applyTo(target).toBuilder().nextInteraction(nextInteraction).build();
    }
}
