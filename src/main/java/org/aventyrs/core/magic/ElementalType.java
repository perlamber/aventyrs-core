package org.aventyrs.core.magic;

/**
 * Which element a {@link MagicType#ELEMENTAL} {@link SpellTree} draws on — the qualifier the
 * catalog always writes after the category ({@code ARSENAL ELEMENTAL (Encantamento/Elemental:
 * Todos)}), enumerated at {@code docs/rules/magias.txt} L15: "magias Elementais – subdivididas em
 * Fogo, Magma, Terra, Natural, Água, Gelo, Ar e Eletricidade".
 *
 * <p>A separate column rather than a constant per element on {@link MagicType} itself, because
 * the two are asked about independently: {@code MagiaAlternativaAbility} exempts a Conjurador by
 * <em>Tipo de Magia</em>, and an Elemental exemption covers Piromancia and Fúria de Tesla alike.
 * Folding the element into the type would silently narrow that.
 *
 * <p>{@link SpellTree#getElementalType()} is {@link java.util.Optional#empty()} for a tree that
 * is not Elemental at all — 9 of the 20 complete trees.
 */
public enum ElementalType {

    FOGO,
    MAGMA,
    TERRA,

    /** See {@link MagicType#NATURAL} — the source document uses "Natural" at both levels. */
    NATURAL,

    AGUA,
    GELO,
    VENTO,
    ELETRICIDADE,

    /**
     * Every element at once, usable only by a resistance. An elemental attack or Magia must name
     * the concrete element it uses.
     */
    TODOS
}
