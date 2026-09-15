package org.aventyrs.core.effect;

import org.aventyrs.core.magic.Spell;

import java.util.Optional;

/**
 * Turns <b>one authored column</b> of a Magia into an applicable {@link SpellEffect} — the
 * construction half of a {@link SpellEffectKind}, which carries a supplier of one.
 *
 * <p><b>Keyed on the column, not on the effect type, and that distinction is load-bearing.</b> A
 * builder is never asked "are you the right kind for this Magia"; it is asked "does this Magia
 * author <em>your</em> column", and answers {@link Optional#empty()} when it does not. That is
 * what keeps this on the right side of {@link Effect}'s own promise that "no central registry or
 * switch statement needs to know a new Effect exists": nothing here enumerates effect classes, and
 * a Magia's own data decides which builder answers.
 *
 * <p><b>Each builder owns its collaborators.</b> {@code HealingEffectBuilder} holds the {@code
 * RestService} that turns a Descanso tier into a figure, with the usual no-arg default — so the
 * casting service holds none of it. That is the point of splitting them out: a service that
 * orchestrates two rolls and a duration should not accumulate a field per effect category.
 *
 * <p>Implementations are stateless beyond their collaborators and safe to build per call; {@link
 * SpellEffectKind} constructs a fresh one each time, the same as {@code
 * SkillType#newInteraction()}.
 */
public interface SpellEffectBuilder {

    /**
     * This builder's effect for spell, or {@link Optional#empty()} when the Magia authors no
     * column of the kind this builder reads.
     *
     * @param spell   the Magia — which may be an {@code AlternateSpellVersion}, since that is a
     *                {@link Spell} like any other and reports its own columns
     * @param context the per-cast facts — see {@link SpellEffectContext}
     */
    Optional<SpellEffect> build(Spell spell, SpellEffectContext context);
}
