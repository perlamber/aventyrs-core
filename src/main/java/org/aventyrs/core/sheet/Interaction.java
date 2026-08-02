package org.aventyrs.core.sheet;

/**
 * Something that can be performed on any {@link Interactable}, like a Perícia roll test,
 * using experience, spending points, etc. Each concrete Interaction owns the logic to
 * compute its own {@link InteractionResult} from the target's characteristics.
 */
public interface Interaction<R extends Interactable> {
    InteractionResult applyTo(R target );
}
