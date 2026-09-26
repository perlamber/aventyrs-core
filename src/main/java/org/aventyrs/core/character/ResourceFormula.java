package org.aventyrs.core.character;

/**
 * Which rulebook formula builds a creature's three Bônus Bases — PV, PD and PM.
 *
 * <p>A player character's are the Módulo Básico's: a flat 10 plus the governing Atributo times its
 * Multiplicador, each pool on its own. A monster's come from {@code criacao-de-monstros.txt}
 * ("Sobre os Bônus Bases"), where the two lesser pools are derived from the first:
 *
 * <ul>
 *   <li>PV = 20 + (Vigor × Multiplicador de PV) — the Multiplicador being its Categoria's;</li>
 *   <li>PD = Metade dos PV + (Instinto × 3);</li>
 *   <li>PM = Metade dos PV + (Foco × 2).</li>
 * </ul>
 *
 * <p>"Metade dos PV" is read as half the creature's <i>maximum</i> PV, everything included (the
 * +2PV each Habilidade Monstruosa grants, a Murchar's lowered Multiplicador) — the text names no
 * narrower quantity. The ×3/×2 stay ordinary Multiplicadores ({@code Character#determinationMultiplier}
 * / {@code #manaMultiplier}), so a Habilidade raising one ("Multiplicador de PD +1") still lands.
 *
 * <p>Read by {@code HitPointsServiceImpl}, {@code DeterminationPointsServiceImpl} and {@code
 * MagicPointsServiceImpl}; carried on {@link Character} rather than branched on the sheet type so
 * their {@code Character}-only overloads answer correctly too.
 */
public enum ResourceFormula {

    /** The Módulo Básico's pools: {@code 10 + Atributo × Multiplicador}, each independent. */
    CHARACTER(10),

    /** A monster's pools: {@code 20 + Vigor × Multiplicador} PV, and PD/PM built on half of it. */
    MONSTER(20);

    private final int baseHitPoints;

    ResourceFormula(final int baseHitPoints) {
        this.baseHitPoints = baseHitPoints;
    }

    /** The flat PV every creature under this formula starts from, before Vigor. */
    public int getBaseHitPoints() {
        return baseHitPoints;
    }

    /** Whether PD and PM are built on half the PV maximum instead of a flat base. */
    public boolean derivesLesserPoolsFromHitPoints() {
        return this == MONSTER;
    }
}
