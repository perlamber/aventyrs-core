package org.aventyrs.core.character;

/**
 * The sacred or profane nature of a hit (core 0.0.89) — "Dano Físico Profano", "Dano Mágico Profano", "Dano causado
 * é Sagrado em adição aos seus tipos". A qualifier, never a type of its own: a Profano hit is still Físico or Mágico,
 * which is what RD/RM/RDS read ({@link DamageType}). What reads the qualifier: an immunity or Meio-Dano scoped to it
 * ({@link DamageScope#sanctity}), and a reduction of that nature ({@code ModifierType#PROFANE_DAMAGE_REDUCTION} /
 * {@code #SACRED_DAMAGE_REDUCTION}).
 */
public enum DamageSanctity {

    /** Profano — Definhar, Veneno Vampírico, Toque Sombrio, a Turmalina Obscura's weapon. */
    PROFANO,

    /** Sagrado — an Opala Purificadora's weapon. */
    SAGRADO
}
