package org.aventyrs.core.character;

/**
 * The <b>nature</b> of a hit — the first half of the ruleset's damage taxonomy (table update, 2026-10-12). The other
 * half is a qualifier carried by {@link DamageDescriptor}: an element ({@code ElementalType}) or a sanctity ({@link
 * DamageSanctity} — Profano, Sagrado, Umbral). The rules' twelve names are nature × qualifier:
 * <ul>
 *   <li>Físico — {@link #FISICO}; Físico Profano/Sagrado/Umbral — {@link #FISICO} + sanctity;</li>
 *   <li>Elemental — {@link #ELEMENTAL} + element (the pre-update "Físico-Elemental");</li>
 *   <li>Mágico Elemental — {@link #MAGICO} + element; Mágico Profano/Sagrado/Umbral — {@link #MAGICO} + sanctity.
 *       A Mágico hit is always qualified — there is no plain Dano Mágico any more;</li>
 *   <li>Primordial — {@link #PRIMORDIAL}; Danos de Queda — {@link #QUEDA}; Danos de Asfixia — {@link #ASFIXIA}.</li>
 * </ul>
 *
 * <p><b>Each reduction reaches only the natures its rules text names</b> ({@code
 * docs/rules/defesas-e-resistencias.txt}), resolved by {@code DamageServiceImpl#computeFinalDamage}
 * off the hit's effective type (the descriptor's when there is one):
 * <ul>
 *   <li>RD ({@code DAMAGE_REDUCTION}) — {@link #FISICO} only, or a hit left untyped;</li>
 *   <li>RDS ({@code DAMAGE_TAKEN_REDUCTION}) — every nature that {@link #takesOrdinaryMitigation()};</li>
 *   <li>RM ({@code MAGIC_REDUCTION}) — {@link #MAGICO}, whatever its qualifier;</li>
 *   <li>RE — any hit carrying the element resisted ({@link #ELEMENTAL} or a Mágico Elemental);</li>
 *   <li>RA — every hit but a {@link #isPrimordial() Primordial} one ("reduz os Danos Físicos, Mágicos e
 *       Elementais", V19 20261012).</li>
 * </ul>
 * An immunity or a scoped Meio-Dano ({@code DamageScope}) is judged against the same type.
 * Corte/Perfuração/Impacto are not modelled: nothing in the Talento catalog needs them.
 */
public enum DamageType {
    FISICO,
    MAGICO,
    /** RE-only: never RD, never RM (table ruling, 2026-10-12). Always names its element. */
    ELEMENTAL,
    PRIMORDIAL,
    /** Danos de Queda — Dano Primordial ({@code acoes-de-combate.txt}): nothing reduces it (table ruling, 2026-10-13). */
    QUEDA,
    /** Danos de Asfixia — Dano Primordial ({@code acoes-de-combate.txt}): nothing reduces it (table ruling, 2026-10-13). */
    ASFIXIA;

    /** Whether a hit of this nature must name an element ({@link #ELEMENTAL}). */
    public boolean requiresElement() {
        return this == ELEMENTAL;
    }

    /** Whether a hit of this nature may name an element — Elemental, or a Mágico Elemental. */
    public boolean admitsElement() {
        return this == ELEMENTAL || this == MAGICO;
    }

    /**
     * Whether RDS, Pele de Pedra and a Magia-scoped RM reach this nature — everything that is not {@link
     * #isPrimordial() Primordial} ("não-PRIMORDIAL"). {@code null} (an untyped hit) counts as Físico, so true.
     */
    public static boolean takesOrdinaryMitigation(final DamageType type) {
        return type == null || type.takesOrdinaryMitigation();
    }

    /** See {@link #takesOrdinaryMitigation(DamageType)}. */
    public boolean takesOrdinaryMitigation() {
        return !isPrimordial();
    }

    /** {@link #PRIMORDIAL}, and the two natures that deal it ({@link #QUEDA}, {@link #ASFIXIA}) — RA does not reach it. */
    public boolean isPrimordial() {
        return this == PRIMORDIAL || isUnmitigated();
    }

    /** {@link #QUEDA}/{@link #ASFIXIA}: Dano Primordial that no resistance and no Meio-Dano reduces. */
    public boolean isUnmitigated() {
        return this == QUEDA || this == ASFIXIA;
    }
}
