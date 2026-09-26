package org.aventyrs.core.monster;

import lombok.Getter;
import org.aventyrs.core.skill.DifficultyLevel;

/**
 * A monster's Categoria — Presa, Deviante, Predador, Apex or Abominação — and the ceilings it
 * sets. From {@code docs/rules/criacao-de-monstros.txt}, "Determinando sua Categoria".
 *
 * <p><b>The Categoria is never chosen; it is read off the Grau de Poder</b> ({@link
 * #forPowerDegree(int)}). "O que determina a Categoria de um Monstro é seu Grau de Poder, a
 * progressão de uma criatura permite que ela transite entre os diferentes tipos" — a Goblin grown
 * from GP 11 to GP 12 becomes a Deviante with nothing else touched.
 *
 * <table>
 *   <caption>The rules table, column by column</caption>
 *   <tr><th></th><th>GP</th><th>GD máximo</th><th>Atributo máximo</th><th>× PV</th><th>Modelos</th></tr>
 *   <tr><td>Presas</td><td>0 – 11</td><td>Difícil</td><td>6</td><td>5</td><td>3</td></tr>
 *   <tr><td>Deviantes</td><td>12 – 25</td><td>Muito Difícil</td><td>7</td><td>6</td><td>4</td></tr>
 *   <tr><td>Predadores</td><td>26 – 45</td><td>Improvável</td><td>8</td><td>8</td><td>5</td></tr>
 *   <tr><td>Apex</td><td>46 – 60</td><td>Inimaginável</td><td>9</td><td>10</td><td>6</td></tr>
 *   <tr><td>Abominações</td><td>61+</td><td>Milagre</td><td>10</td><td>12</td><td>8</td></tr>
 * </table>
 *
 * <p>Two more columns come from the prose above the table: the Pontos de Ação bonus ("aumenta em
 * +1 em Predadores, em +2 em Apexes e +3 em Abominações") and the extra Ego points to allocate
 * ("Deviantes recebem +1 ponto adicional … +3 se forem Predadores, +6 … Apex, +11 …
 * Abominações" — the running totals, not the per-step increments).
 *
 * <p>The declaration order is the power order, so {@code compareTo} answers "at least a
 * Predador" — which is exactly how a Habilidade's tier and its Aprimoramentos are gated.
 */
@Getter
public enum MonsterCategory {

    PRESA(0, DifficultyLevel.HARD, 6, 5, 3, 0, 0),
    DEVIANTE(12, DifficultyLevel.VERY_HARD, 7, 6, 4, 0, 1),
    PREDADOR(26, DifficultyLevel.UNLIKELY, 8, 8, 5, 1, 3),
    APEX(46, DifficultyLevel.UNIMAGINABLE, 9, 10, 6, 2, 6),
    ABOMINACAO(61, DifficultyLevel.MIRACLE, 10, 12, 8, 3, 11);

    /** The lowest Grau de Poder that makes a monster this Categoria. */
    private final int minimumPowerDegree;

    /**
     * "GD Máximo em Perícia" — the highest Grau de Dificuldade its own progression can reach.
     * Habilidades Monstruosas may push past it ("Aprimoramentos Monstruosos podem fazer com que a
     * GD máxima em perícia seja superada"), so {@link MonsterRules} clamps before applying them.
     */
    private final DifficultyLevel maximumSkillLevel;

    /**
     * "Atributo Base Máximo" — the ceiling on an Atributo's base plus Bônus Racial. The creation
     * budget caps a base at 5 on its own; this column only bites once Habilidades start granting
     * Bônus Racial.
     */
    private final int maximumAttribute;

    /** "Multiplicador de PV" — the Vigor multiplier in {@code 20 + Vigor × Multiplicador}. */
    private final int lifeMultiplier;

    /** "Quantidade de Modelos" — how many Modelos a monster of this Categoria may hold. */
    private final int maximumModels;

    /** Pontos de Ação on top of the standard 3. */
    private final int bonusActionPoints;

    /** Ego points to allocate on top of every Ego's base 2 — the rules' running total. */
    private final int bonusEgoPoints;

    MonsterCategory(final int minimumPowerDegree, final DifficultyLevel maximumSkillLevel, final int maximumAttribute,
                    final int lifeMultiplier, final int maximumModels, final int bonusActionPoints,
                    final int bonusEgoPoints) {
        this.minimumPowerDegree = minimumPowerDegree;
        this.maximumSkillLevel = maximumSkillLevel;
        this.maximumAttribute = maximumAttribute;
        this.lifeMultiplier = lifeMultiplier;
        this.maximumModels = maximumModels;
        this.bonusActionPoints = bonusActionPoints;
        this.bonusEgoPoints = bonusEgoPoints;
    }

    /**
     * The Categoria a monster of this Grau de Poder belongs to. A negative GP is treated as 0 —
     * the table starts there, and nothing in the rules is weaker than a Presa.
     */
    public static MonsterCategory forPowerDegree(final int powerDegree) {
        MonsterCategory result = PRESA;
        for (MonsterCategory category : values()) {
            if (powerDegree >= category.minimumPowerDegree) {
                result = category;
            }
        }
        return result;
    }

    /** Whether this Categoria is {@code other} or above — "Aprimoramentos dos Predadores" reach a Predador and every Categoria past it. */
    public boolean isAtLeast(final MonsterCategory other) {
        return compareTo(other) >= 0;
    }
}
