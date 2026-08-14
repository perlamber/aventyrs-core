package org.aventyrs.core.effect;

import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.Interaction;

/**
 * The parent of every "Efeito" this core can eventually model — a Corrente de Efeitos
 * ({@link EffectChain}), an Efeito Crítico ({@link CriticalEffect}), or any future
 * category alongside them. Extending {@link Interaction}&lt;{@link CharacterSheet}&gt;
 * is deliberate, not incidental: it's what makes every concrete Effect, once written,
 * pluggable through {@link CharacterSheet#receiveInteraction} the exact same
 * zero-touch way a concrete {@code <Skill>Interaction} already is — no central registry
 * or switch statement needs to know a new Effect exists. See {@code
 * org.aventyrs.core.effect} package-info for the Skill -> Damage -> EffectChain ->
 * CriticalEffect pipeline this fits into, and how {@link
 * org.aventyrs.core.sheet.InteractionResult#getNextInteraction()} chains one stage into
 * the next.
 */
public interface Effect extends Interaction<CharacterSheet> {
    String getDescription();
}
