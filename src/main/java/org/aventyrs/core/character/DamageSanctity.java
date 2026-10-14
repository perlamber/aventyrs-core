package org.aventyrs.core.character;

/**
 * The sacred, profane or umbral nature of a hit (core 0.0.89; Umbral since the 2026-10-12 taxonomy) — "Dano Físico Profano", "Dano Mágico Profano", "Dano causado
 * é Sagrado em adição aos seus tipos". A qualifier, never a type of its own: a Profano hit is still Físico or Mágico,
 * which is what RD/RM/RDS read — and a Mágico hit must carry either this or an element ({@link DamageType}). What reads the qualifier: an immunity or Meio-Dano scoped to it
 * ({@link DamageScope#sanctity}), and a reduction of that nature ({@code ModifierType#PROFANE_DAMAGE_REDUCTION} /
 * {@code #SACRED_DAMAGE_REDUCTION}).
 */
public enum DamageSanctity {

    /** Profano — Definhar, Veneno Vampírico, Toque Sombrio, a Turmalina Obscura's weapon. */
    PROFANO,

    /** Sagrado — an Opala Purificadora's weapon. */
    SAGRADO,

    /**
     * Umbral — "Dano Mágico Umbral" (the Força Umbral Magias), added with the damage taxonomy update (2026-10-12).
     * ⚠️ No reduction of Umbral damage exists yet: no source in the catalog grants one, so no {@code ModifierType}.
     */
    UMBRAL
}
