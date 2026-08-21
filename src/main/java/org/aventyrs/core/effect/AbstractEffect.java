package org.aventyrs.core.effect;

import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.Interaction;
import org.aventyrs.core.sheet.InteractionResult;

/**
 * The successor-carrying half of every concrete {@link Effect} — the state behind {@link
 * Interaction#getNextInteraction()}, which an interface can't hold itself.
 *
 * <p>An Efeito on its own is a single step: it happens, and the pipeline ends. But one attack can
 * genuinely trigger several — an Efeito Crítico <i>and</i> a Corrente de Efeitos, or more than
 * one of either — and the two conditions are independent (an attack can trigger a Corrente
 * without a critical, and a critical without a Corrente). Rather than a caller applying each by
 * hand in the right order, an assembled chain is one object: {@link #chainInto} links a
 * successor, {@link #reportChain} copies it onto the result, and {@code CharacterSheet
 * #receiveInteraction}'s existing drain loop walks the whole thing with no change at all.
 *
 * <pre>{@code
 * // built back-to-front, so each stage already knows its successor
 * Sangramento bleed = new Sangramento(critical);
 * bleed.chainInto(new Definhar(target));
 * }</pre>
 *
 * <p>Subclassing this is optional and adds nothing but the successor: an Effect with no chaining
 * to do can keep implementing {@link CriticalEffect}/{@link EffectChain} directly.
 */
public abstract class AbstractEffect implements Effect {

    private Interaction<CharacterSheet> nextInteraction;

    @Override
    public Interaction<CharacterSheet> getNextInteraction() {
        return nextInteraction;
    }

    /**
     * Links nextInteraction as this Effect's successor, returning {@code this} so a chain reads
     * back-to-front in one expression. A second call replaces the first — an Effect has exactly
     * one successor, since the chain is a line, not a tree.
     */
    public AbstractEffect chainInto(final Interaction<CharacterSheet> nextInteraction) {
        this.nextInteraction = nextInteraction;
        return this;
    }

    /**
     * Copies this Effect's successor onto result, so the pipeline's drain loop reaches it. Every
     * subclass's {@code applyTo} passes its own builder through this before building —
     * forgetting to is what would silently truncate a chain.
     */
    protected InteractionResult.InteractionResultBuilder reportChain(final InteractionResult.InteractionResultBuilder result) {
        return result.nextInteraction(nextInteraction);
    }
}
