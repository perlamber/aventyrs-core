package org.aventyrs.core.sheet;

import lombok.Getter;

import java.util.Optional;

/**
 * Ongoing per-Rodada curse damage from Definhar (see {@code
 * org.aventyrs.core.effect.Definhar}) — registered on a target via {@link
 * CharacterSheet#applyEffect(TemporaryEffect)}. Counts down in Rodadas via its shared
 * superclass {@link TemporaryEffect} (see there for the {@code remainingRounds} shape it
 * shares with {@link TemporaryBonus}/{@link Bleeding}/{@link ManaDrain}), advanced by
 * {@link CharacterSheet#tickTemporaryEffects()} — called once per Rodada by {@link
 * CharacterSheet#finishTurn()}, itself not called automatically by anything yet; see that
 * method's own javadoc.
 *
 * <p>Unlike {@link Bleeding} (plain {@link CharacterSheet#applyDamage(int)}), each Rodada
 * drains via {@link CharacterSheet#applyCurseDamage(int)} instead — Definhar's own rules
 * text calls its damage "Dano Físico Profano" and explicitly labels the whole effect an
 * "Efeito de Maldição" (a Curse Effect), the same reasoning {@code
 * org.aventyrs.core.effect.RealExecution} inferred for its own "imediatamente destruído"
 * clause, confirmed here directly rather than inferred: curse damage bypasses Shield
 * points. A general "maldição" effect classification (e.g. for {@code
 * org.aventyrs.core.race.Gorgona}'s own still-unbuilt "fortalecidas por maldições" trait)
 * still doesn't exist — this only wires up the one concrete mechanical consequence
 * Definhar's own text needs today.
 *
 * <p>{@link #isCumulative()} is overridden to return false — Definhar's own rules text is
 * explicitly "não cumulativo", so {@link CharacterSheet#applyEffect} replaces any existing
 * Withering with the new one instead of letting two run concurrently.
 */
@Getter
public class Withering extends TemporaryEffect {
    private final int valuePerRound;

    public Withering(final int valuePerRound, final Optional<Integer> remainingRounds) {
        super(remainingRounds.orElse(null));
        this.valuePerRound = valuePerRound;
    }

    @Override
    boolean isCumulative() {
        return false;
    }

    @Override
    void applyRoundEffect(final CharacterSheet sheet) {
        sheet.applyCurseDamage(valuePerRound);
    }
}
