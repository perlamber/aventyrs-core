package org.aventyrs.core.magic;

/**
 * Frenesi Arcano's two options for the next Magia its holder casts — granted as a one-cast budget
 * ({@code CombatantSheet#grantEnhancedAttacks(this, 1)}) and spent by {@code
 * SpellCastingServiceImpl#castSpell}, which also adds its "+3PA" to the cast's Tempo de Ativação.
 */
public enum SpellEmpowerment {

    /**
     * "Você pode aumentar o Dano Base de suas magias em +1d6, então magias capazes de infligir 3d6
     * pontos de dano são maximizadas." Read as: the extra die is added, and a Magia that would then
     * roll more than 3d6 — the most a Magia's Dano Base reaches — keeps its dice and deals their
     * maximum instead.
     */
    DANO,

    /** "Alternativamente você pode aumentar a Duração de seus Encantamentos e Maldições em +2 Rodadas." */
    DURACAO;

    /** Frenesi Arcano's "Custo de Ativação: +3PA", added to the empowered cast. */
    public static final int ACTION_POINT_SURCHARGE = 3;
    /** "+2 Rodadas". */
    public static final int DURATION_BONUS = 2;
    /** "magias capazes de infligir 3d6 pontos de dano são maximizadas". */
    public static final int MAXIMUM_DICE = 3;
}
