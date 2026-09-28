package org.aventyrs.core.sheet;

/**
 * Presença de Carmilla running on its holder — the Poder Vampírico "a cada Rodada você recupera 1PV
 * para cada personagem vivo em Distância Muito Curta que esteja ferido". A marker carrying only the
 * Duração: the recovery needs the Scene's neighbours, which a {@link TemporaryEffect}'s own Rodada
 * tick never sees, so {@code VampiricPresenceService#drain} performs it while this is held.
 */
public class CarmillaPresence extends TemporaryEffect {

    public CarmillaPresence(final int rounds) {
        super(rounds);
    }

    /** A second activation renews the Duração rather than doubling the drain. */
    @Override
    boolean isCumulative() {
        return false;
    }
}
