package org.aventyrs.core.magic;

/**
 * The unit a Magia's {@code Duração:} descriptor is <b>authored</b> in, kept alongside the count
 * rather than converted away at authoring time.
 *
 * <p>Rodadas is the right canonical unit for arithmetic — {@link SpellDuration#inRodadas()}
 * converts — but discarding the authored unit silently breaks the single most common Efeito
 * Crítico in the game. {@code CriticalEffectType#POTENCIALIZAR} (57 of the 145 complete Magias)
 * reads "A duração da magia aumenta em +2d6 <b>unidades</b>", not <i>+2d6 Rodadas</i>: the unit
 * is the Magia's own. On a {@code 1 minuto} Magia stored as a bare {@code 12}, that lands at
 * 1/12th of its real magnitude.
 *
 * <h2>1 minuto = 12 Rodadas</h2>
 *
 * This conversion is stated in <b>none</b> of the three source documents — not {@code
 * magias.txt}, not {@code efeitos-criticos.txt}, not {@code talentos.txt}. It is carried-in
 * ruleset knowledge, which is exactly why it is written down here rather than inlined at a call
 * site.
 */
public enum DurationUnit {

    /** The combat tick everything else converts into. */
    RODADA(1),

    /** 12 Rodadas. Three complete Magias are authored in minutes. */
    MINUTO(12),

    /** 720 Rodadas. One complete Magia is authored in hours ({@code Até 1 Hora}). */
    HORA(720);

    private final int rodadas;

    DurationUnit(final int rodadas) {
        this.rodadas = rodadas;
    }

    /** How many Rodadas one of this unit is worth. */
    public int getRodadas() {
        return rodadas;
    }
}
