package org.aventyrs.core.sheet;

import lombok.Getter;

import java.util.Optional;

/**
 * Ongoing per-Rodada PM loss from Purga-Mana (see {@code
 * org.aventyrs.core.effect.ManaPurge}) — registered on a target alongside ManaPurge's own
 * immediate PM loss, via {@link CombatantSheet#applyEffect(TemporaryEffect)}. Counts down
 * in Rodadas via its shared superclass {@link TemporaryEffect} (see there for the {@code
 * remainingRounds} shape it shares with {@link TemporaryBonus}/{@link Bleeding}), advanced
 * by {@link CombatantSheet#tickTemporaryEffects()} — called once per Rodada by {@link
 * CombatantSheet#finishTurn()}, itself not called automatically by anything yet; see that
 * method's own javadoc.
 *
 * <p>An open-ended (i.e. {@code null} {@code remainingRounds}) ManaDrain is how ManaPurge
 * Maior is represented — its own duration ("até o fim da cena ou 1 minuto, o que for
 * maior") has no fixed Rodada count this core can compute (no scene-end trigger or
 * minute-based time tracking exists), so it simply never expires on its own from ticking
 * alone; only {@link CombatantSheet#recoverMagicPoints(int)} interrupting it (per
 * ManaPurge's own rules text) or a caller explicitly clearing it ends it. ManaPurge
 * Menor's count (equal to the target's Foco) is a real, finite value instead.
 */
@Getter
public class ManaDrain extends TemporaryEffect {
    private final int valuePerRound;

    public ManaDrain(final int valuePerRound, final Optional<Integer> remainingRounds) {
        super(remainingRounds.orElse(null));
        this.valuePerRound = valuePerRound;
    }

    @Override
    void applyRoundEffect(final CombatantSheet sheet) {
        sheet.spendMagicPoints(valuePerRound);
    }
}
