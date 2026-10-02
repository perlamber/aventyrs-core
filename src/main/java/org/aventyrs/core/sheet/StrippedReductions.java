package org.aventyrs.core.sheet;

/**
 * "O alvo desta magia perde sua RD e RM" for a while (core 0.1.0) — Enfadecer's Boneca de Porcelana. While one runs,
 * {@code DamageService} counts no RD and no RM for its holder, whatever grants them; RDS, RA, RE and Meio-Dano are
 * other resistances and stay. Read through {@link CombatantSheet#isStrippedOfReductions()}.
 */
public class StrippedReductions extends TemporaryEffect {

    public StrippedReductions(final int rounds) {
        super(rounds);
    }
}
