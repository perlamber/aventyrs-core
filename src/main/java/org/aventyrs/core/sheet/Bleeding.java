package org.aventyrs.core.sheet;

import lombok.Getter;

import java.util.Optional;

/**
 * Ongoing per-Rodada PV loss from Sangramento (see {@code
 * org.aventyrs.core.effect.Sangramento}) — registered on a target alongside Sangramento's
 * own immediate PV loss, via {@link CharacterSheet#applyEffect(TemporaryEffect)}. Counts down
 * in Rodadas via its shared superclass {@link TemporaryEffect} (see there for the {@code
 * remainingRounds} shape it shares with {@link TemporaryBonus}), advanced by {@link
 * CharacterSheet#tickTemporaryEffects()} — called once per Rodada by {@link
 * CharacterSheet#finishTurn()}, itself not called automatically by anything yet; see that
 * method's own javadoc.
 *
 * <p>An open-ended (i.e. {@code null} {@code remainingRounds}) Bleeding is how Sangramento
 * Maior is represented — its own duration ("até o fim da cena ou 1 minuto, o que for
 * maior") has no fixed Rodada count this core can compute (no scene-end trigger or
 * minute-based time tracking exists), so it simply never expires on its own from ticking
 * alone; only {@link CharacterSheet#heal(int)} interrupting it (per Sangramento's own
 * rules text) or a caller explicitly clearing it ends it. Sangramento Menor's count
 * (equal to the target's Vigor) is a real, finite value instead.
 */
@Getter
public class Bleeding extends TemporaryEffect {
    private final int valuePerRound;

    public Bleeding(final int valuePerRound, final Optional<Integer> remainingRounds) {
        super(remainingRounds.orElse(null));
        this.valuePerRound = valuePerRound;
    }

    @Override
    void applyRoundEffect(final CharacterSheet sheet) {
        sheet.applyDamage(valuePerRound);
    }
}
