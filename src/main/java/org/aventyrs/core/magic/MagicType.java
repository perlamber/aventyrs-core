package org.aventyrs.core.magic;

/**
 * A Categoria de Magia — "o tipo de Mana empregado na conjuração ou ao efeito gerado pela
 * magia" ({@code docs/rules/magias.txt} L13). It is a property of the whole {@link SpellTree},
 * never of an individual Magia: the catalog prints it once, in parentheses after each Árvore's
 * ALL-CAPS heading ({@code PIROMANCIA (Divina/Elemental: Fogo)}), which is why {@link
 * Spell#getPrimaryType()}/{@link Spell#getSecondaryType()} default to delegating to the tree.
 *
 * <p>A tree carries one or two of these. Three of the twenty complete trees are single-typed
 * (Anulação, Ira de Vulcano, Ocultação), which is why the secondary is an {@link
 * java.util.Optional} rather than a required column.
 *
 * <h2>{@link #ELEMENTAL} is always qualified</h2>
 *
 * The catalog never writes a bare "Elemental" — it is always {@code Elemental: Fogo}, {@code
 * Elemental: Magma}, and so on, for 11 of the 20 complete trees. That subdivision is {@link
 * ElementalType}, a separate column on {@link SpellTree}, because without it two Elemental trees
 * would be indistinguishable.
 *
 * <h2>{@link #NATURAL} is used two ways by the source document, which does not reconcile them</h2>
 *
 * L15 lists Natural as one of the Elemental subdivisions ("magias Elementais – subdivididas em
 * Fogo, Magma, Terra, <b>Natural</b>, Água, Gelo, Ar e Eletricidade"), yet three trees tag it
 * standalone as a top-level category ({@code Natural/Invocação}, {@code Natural/Divina}, {@code
 * Encantamento/Natural}). It is kept here <b>and</b> in {@link ElementalType} for that reason —
 * the three standalone trees author this constant, and an {@code Elemental: Natural} tree (only
 * the Umbral draft's Devorador de Mundos, today) would author the other. Settling which reading
 * is authoritative is a rules question, not a code one.
 */
public enum MagicType {

    /** Deusas Primordiais, the destruction/death half. */
    PROFANA,

    /** Deusas Primordiais, the creation/life half — Preces Divinas. */
    DIVINA,

    /** "convocar ou criar criaturas para servir e auxiliar o Conjurador". */
    INVOCACAO,

    /** "manipular o Mana existente em outros seres e objetos". */
    ENCANTAMENTO,

    /** See the class javadoc — the document uses this both as a top-level type and as an {@link ElementalType}. */
    NATURAL,

    /** Always qualified by an {@link ElementalType} — see the class javadoc. */
    ELEMENTAL,

    /** "utilizam o AEther ao invés do Mana, para gerar efeitos neutros ou brutos". */
    PRIMORDIAL,

    /**
     * "capazes de manipular o espaço-tempo". Required by two fully-specified trees of the
     * complete list — {@code TEMPO (Encantamento/Temporal)} and {@code TRANSPORTE
     * (Temporal/Invocação)} — not only by the Umbral draft.
     */
    TEMPORAL,

    /**
     * Sombras da Umbra. Every one of the seven Umbral trees carries it, as does the <i>Força
     * Umbral</i> Talento gating them. Those 44 Magias are <b>not authored</b> — but their
     * descriptors are only <em>partly</em> blank ({@code GD da Conjuração} is the one missing from
     * all 44, and even that is derivable from the rung). What actually blocks them is that the
     * Talento gating their acquisition does not exist. See {@code MagicTree}'s javadoc.
     */
    UMBRAL
}
