package org.aventyrs.core.subordinate;

import lombok.Getter;

/**
 * What a Subordinado grants — one of its grade's two options, picked when it is gained (⚠️ a reading: "escolhidos
 * entre" names no moment; core 0.0.92). {@link #isCombatOnly()} follows "Bispos, Cavaleiros e Torres influenciam os
 * Personagens apenas em cenas de Combate … Rainhas entregam bônus em Ação continuamente, mas auxiliam em Iniciativa
 * apenas em cenas de estresse"; a Peão's and a Rei's hold everywhere.
 */
@Getter
public enum SubordinateBenefit {

    /** "fazendo os recuperar 2PV por Rodada". */
    BISPO_REGENERATION(SubordinateGrade.BISPO, true),
    /** "ou concedendo Roubo de Vida 1 aos seus ataques e magias". */
    BISPO_LIFE_STEAL(SubordinateGrade.BISPO, true),
    /** "fornecendo Vantagem em rolagens de Perícias de Ataque". */
    CAVALEIRO_ATTACK(SubordinateGrade.CAVALEIRO, true),
    /** "… ou Dano". */
    CAVALEIRO_DAMAGE(SubordinateGrade.CAVALEIRO, true),
    /** "concedendo-os Vantagem em rolagens de Perícias … não aplicável à Perícias de Ataque e Esquiva e Aparar". */
    PEAO_SKILL(SubordinateGrade.PEAO, false),
    /**
     * "ou reduzindo a margem crítica de rolagens de Perícias em -2" — ⚠️ read as the Margem Crítica Menor widening by two
     * numbers, on the same Perícias.
     */
    PEAO_CRITICAL(SubordinateGrade.PEAO, false),
    /** "recebem +1PA" — continuously. */
    RAINHA_ACTION_POINT(SubordinateGrade.RAINHA, false),
    /** "ou aumentam sua Iniciativa em +2" — only in "cenas de estresse", read as a Cena de Combate. */
    RAINHA_INITIATIVE(SubordinateGrade.RAINHA, true),
    /** "2 Pontos Temporários em Ego, escolhido entre Sorte" — once a day, renewed by a Descanso Longo. */
    REI_SORTE(SubordinateGrade.REI, false),
    /** "… e Autocontrole". */
    REI_AUTOCONTROLE(SubordinateGrade.REI, false),
    /** "Bônus de +2 nas Defesas do personagem". */
    TORRE_DEFESAS(SubordinateGrade.TORRE, true),
    /** "ou RA". */
    TORRE_RA(SubordinateGrade.TORRE, true);

    /** Bispo's "2PV por Rodada". */
    public static final int REGENERATION = 2;
    /** Bispo's "Roubo de Vida 1". */
    public static final int LIFE_STEAL = 1;
    /** Peão's widened Margem Crítica Menor, in numbers. */
    public static final int CRITICAL_MARGIN = 2;
    /** Rainha's "+1PA". */
    public static final int ACTION_POINTS = 1;
    /** Rainha's "Iniciativa em +2". */
    public static final int INITIATIVE = 2;
    /** Rei's "2 Pontos Temporários em Ego". */
    public static final int EGO_POINTS = 2;
    /** Torre's "+2 nas Defesas". */
    public static final int DEFESAS = 2;

    private final SubordinateGrade grade;
    private final boolean combatOnly;

    SubordinateBenefit(final SubordinateGrade grade, final boolean combatOnly) {
        this.grade = grade;
        this.combatOnly = combatOnly;
    }
}
