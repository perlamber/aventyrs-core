package org.aventyrs.core.sheet;

/**
 * A combatant unable to breathe — submerged, or in a gas (acoes-de-combate.txt, Dano de Asfixia). Held on the sheet
 * from the moment breathing stops until {@code AsphyxiaService#breathe}; only {@code AsphyxiaService} advances it.
 *
 * @param breathlessRounds   Rodadas spent without air so far
 * @param instancesSuffered  how many instances of Dano de Asfixia have landed
 * @param submerged          whether the air was lost under water — what {@code BestialFeat#HERANCA_CETACEA}'s
 *                           doubled breath reads (core 0.1.7.23)
 */
public record Asphyxia(int breathlessRounds, int instancesSuffered, boolean submerged) {

    /** Breathing has just stopped, out of the water. */
    public static final Asphyxia STARTED = new Asphyxia(0, 0, false);

    /** Breathing has just stopped under water. */
    public static final Asphyxia STARTED_SUBMERGED = new Asphyxia(0, 0, true);

    /** The same count, out of the water — the form every caller before 0.1.7.23 used. */
    public Asphyxia(final int breathlessRounds, final int instancesSuffered) {
        this(breathlessRounds, instancesSuffered, false);
    }

    /** The next count, keeping where the air was lost. */
    public Asphyxia advanced(final int rounds, final int instances) {
        return new Asphyxia(rounds, instances, submerged);
    }
}
