package org.aventyrs.core.sheet;

/**
 * Ferida Profunda Menor — "Efeitos de cura são reduzidos à metade por 2 Rodadas (cumulativo)".
 * Each one held halves a heal once more, so two quarter it; read by {@link CombatantSheet#heal}.
 */
public class HalvedHealing extends TemporaryEffect {

    public HalvedHealing(final int rounds) {
        super(rounds);
    }
}
