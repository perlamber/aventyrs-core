package org.aventyrs.core.magic;

/**
 * How a {@link SpellDamage}'s effect scales with the caster's Foco — the {@code "Metade do Foco"}
 * / {@code "Foco"} term the catalog writes into a Magia's numeric effect.
 *
 * <p>{@link #HALF} is by far the common case (every Magia the catalog currently authors a {@link
 * SpellDamage} for). {@link #FULL} is what {@code
 * org.aventyrs.core.ability.FocusAbility#MAGIA_PODEROSA} upgrades a {@code HALF} term to on the
 * caster's first Magia of a Rodada — see {@code
 * org.aventyrs.core.magic.SpellCastingService#resolvePrimaryDamage}. {@link #NONE} is a flat
 * dice-only figure ("2d6 Pontos de Dano"), kept so such an effect has a representation even
 * though no authored Magia needs it yet.
 */
public enum FocusScaling {
    NONE,
    HALF,
    FULL
}
