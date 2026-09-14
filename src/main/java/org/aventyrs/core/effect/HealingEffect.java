package org.aventyrs.core.effect;

/**
 * A {@link SpellEffect} that restores Pontos de Vida — the category Vida's principal branch
 * fills, and the one this core implements fully.
 *
 * <p>{@link SpellHealingEffect} is the concrete form, covering every rung of a healing branch
 * from one authored {@code SpellHealing}. A recovery that <i>drips</i> rather than landing at
 * once needs no new category either: {@code org.aventyrs.core.sheet.Regeneration} is already a
 * per-Rodada healing {@code TemporaryEffect}, so a Magia of the Regeneração tree would register
 * one from inside its own {@code applyTo}, the way {@link Definhar} registers a {@code
 * Withering}.
 *
 * <p>A marker on top of {@link SpellEffect}, like {@link EffectChain} is on {@link Effect}:
 * nothing should be inferred from it beyond "this Effect heals". How much, and under what
 * condition, is authored data rather than anything this interface states.
 */
public interface HealingEffect extends SpellEffect {
}
