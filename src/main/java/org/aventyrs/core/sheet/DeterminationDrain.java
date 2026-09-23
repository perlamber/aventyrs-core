package org.aventyrs.core.sheet;

import lombok.Getter;

/**
 * Excruciante's ongoing half — "e 1PD adicional por Rodada" — the PD twin of {@link Bleeding}
 * and {@link ManaDrain}. {@code null} rounds is open-ended ("até o fim da cena ou 1 minuto, o que
 * for maior"), which, like Sangramento Maior's, lasts until healed. Interrupted by any heal
 * ({@code CombatantSheet#heal}), per the clause's own "Efeitos de cura interrompem a perda".
 */
@Getter
public class DeterminationDrain extends TemporaryEffect {

    private final int valuePerRound;

    public DeterminationDrain(final int valuePerRound, final Integer remainingRounds) {
        super(remainingRounds);
        this.valuePerRound = valuePerRound;
    }

    @Override
    void applyRoundEffect(final CombatantSheet sheet) {
        sheet.spendDeterminationPoints(valuePerRound);
    }
}
