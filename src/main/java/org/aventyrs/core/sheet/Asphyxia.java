package org.aventyrs.core.sheet;

/**
 * A combatant unable to breathe — submerged, or in a gas (acoes-de-combate.txt, Dano de Asfixia). Held on the sheet
 * from the moment breathing stops until {@code AsphyxiaService#breathe}; only {@code AsphyxiaService} advances it.
 *
 * @param breathlessRounds   Rodadas spent without air so far
 * @param instancesSuffered  how many instances of Dano de Asfixia have landed
 */
public record Asphyxia(int breathlessRounds, int instancesSuffered) {

    /** Breathing has just stopped. */
    public static final Asphyxia STARTED = new Asphyxia(0, 0);
}
