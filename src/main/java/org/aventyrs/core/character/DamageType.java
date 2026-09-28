package org.aventyrs.core.character;

/**
 * The kinds of dano this ruleset distinguishes, carried by {@link DamageBonus} and passed to
 * {@code DamageService#calculateFinalDamage}.
 *
 * <p><b>Each reduction reaches only the types its rules text names</b> ({@code
 * docs/rules/defesas-e-resistencias.txt}), resolved by {@code DamageServiceImpl#computeFinalDamage}
 * off the hit's effective type (the descriptor's when there is one):
 * <ul>
 *   <li>RD ({@code DAMAGE_REDUCTION}) — {@link #FISICO} only, or a hit left untyped;</li>
 *   <li>RDS ({@code DAMAGE_TAKEN_REDUCTION}) — everything but {@link #PRIMORDIAL};</li>
 *   <li>RM ({@code MAGIC_REDUCTION}) — {@link #MAGICO} only;</li>
 *   <li>RE — {@link #ELEMENTAL}/{@link #FISICO_ELEMENTAL} of the element resisted;</li>
 *   <li>RA — every hit.</li>
 * </ul>
 * An immunity or a scoped Meio-Dano ({@code DamageScope}) is judged against the same type.
 * Corte/Perfuração/Impacto are not modelled: nothing in the Talento catalog needs them.
 */
public enum DamageType {
    FISICO,
    MAGICO,
    ELEMENTAL,
    FISICO_ELEMENTAL,
    PRIMORDIAL
}
