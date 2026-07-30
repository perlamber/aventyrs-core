package org.aventyrs.core.sheet;

import org.aventyrs.core.character.CharacterStatus;

import lombok.Builder;
import lombok.Getter;

/**
 * The outcome of an {@link Interaction} applied via {@link Interactable#receiveInteraction}.
 * Different Interactions fill in different fields — e.g. a damage-dealing Interaction sets
 * {@code resultStatus}, while a Perícia test sets {@code skillRollBonus} — the rest stay
 * {@code null} when not applicable to that particular Interaction.
 */
@Getter @Builder
public class InteractionResult {
    Interactable nextInteractable;
    CharacterStatus resultStatus;

    /** The Perícia roll bonus computed by a skill-test Interaction (e.g. AttentionInteraction). */
    Integer skillRollBonus;
}
