package org.aventyrs.core.effect;

import java.util.Optional;

import org.aventyrs.core.magic.Spell;

/**
 * Builds a {@link BodyChangeEffect} from a Magia's {@code Spell#getBodyChange()} (core 0.0.99). One instance per
 * direction: {@link SpellEffectKind#DEFENSIVE} holds the one answering a beneficial change, {@link
 * SpellEffectKind#OFFENSIVE} the one answering a harmful change, so a column always lands in exactly one category.
 */
public class BodyChangeEffectBuilder implements SpellEffectBuilder {

    private final boolean harmful;

    /** @param harmful whether this builder answers a harmful change (true) or a beneficial one (false) */
    public BodyChangeEffectBuilder(final boolean harmful) {
        this.harmful = harmful;
    }

    @Override
    public Optional<SpellEffect> build(final Spell spell, final SpellEffectContext context) {
        return spell.getBodyChange()
                .filter(change -> change.isHarmful() == harmful)
                .map(change -> BodyChangeEffect.of(spell, change, context));
    }
}
