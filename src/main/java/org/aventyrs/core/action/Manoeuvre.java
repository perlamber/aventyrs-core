package org.aventyrs.core.action;

/**
 * A named combat manoeuvre — an action the rules give its own cost, allowance and consequences,
 * as opposed to the plain "spend Pontos de Ação, roll a Perícia" default.
 *
 * <p>It lives in {@code org.aventyrs.core.action} beside {@link ActionProfile} and {@link
 * ActionPointsService} rather than in {@code org.aventyrs.core.skill}, because a manoeuvre names
 * an <b>action</b> and has to be legible from both halves of one: the attack path reads it off
 * {@code org.aventyrs.core.skill.SkillRoll}, and the movement path off {@code
 * org.aventyrs.core.character.services.MovementReactionService}. An Investida is both at once,
 * which is the whole reason this type exists rather than a boolean on either side.
 *
 * <p><b>{@code null} is the ordinary case</b> — an ordinary attack, an ordinary movement — and
 * never means "not an Investida". Same three-state discipline {@code SkillRoll}'s {@code
 * targetValue} and {@code actionCost} keep: "the caller didn't name a manoeuvre" is a different
 * answer from "the caller named a different one".
 *
 * <p><b>One constant, deliberately.</b> The catalog names other manoeuvres — <i>Reposicionar</i>
 * ({@code MobilidadeFeat#MOVIMENTO_RAPIDO}, {@code EscudeiroFeat#DEFESA_TARTARUGA}, {@code
 * BootsItem}'s Sandálhas do Corredor) and the <i>Movimento Acrobático</i> ({@code
 * MobilidadeFeat#MOVIMENTO_ACROBATICO}) — but nothing else about either is modelled: neither has
 * a cost, an allowance or a distance in this core, so a constant for one would be a name with no
 * mechanism behind it. Add each with the rest of its own machinery, not ahead of it.
 */
public enum Manoeuvre {

    /**
     * Investida — a movement and an Ataque Corpo-a-Corpo bought as one action. Costs {@code
     * ChargeService#BASE_ACTION_POINT_COST}, covers {@code ChargeService#BASE_MOVEMENT_MULTIPLIER}
     * times the mover's Movimento Base, adds a flat bonus to the dano roll on a hit and a Redutor
     * to the charger's Defesas on a miss.
     *
     * <p>The manoeuvre's own rules text is in no document under {@code docs/rules/} — see {@code
     * ChargeService} for which of its figures are authored and which are read off the constants
     * that reference it.
     */
    INVESTIDA
}
