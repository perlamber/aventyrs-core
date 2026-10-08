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
 * <p><b>Each constant arrives with its machinery.</b> The catalog names one more manoeuvre — the
 * <i>Movimento Acrobático</i> ({@code MobilidadeFeat#MOVIMENTO_ACROBATICO}) — but nothing about it
 * is modelled: no cost, allowance or distance, so a constant for it would be a name with no
 * mechanism behind it. Add it with the rest of its own machinery, not ahead of it.
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
    INVESTIDA(null),

    /**
     * Reposicionar — a 1UD step bought as an Ação Livre ({@code RepositionService}). It provokes no
     * movement Reações and is refused in Terreno Difícil. Table ruling (2026-09-23): no document
     * under {@code docs/rules/} defines it; they only name it.
     */
    REPOSICIONAR(ActionKind.MOVEMENT),

    /**
     * Agarrar — 2PA, an Ataque Corpo-a-Corpo against the target's Defesa that deals no damage and
     * triggers no Efeito Crítico or Corrente; success leaves the target Agarrado with the captor as
     * the source ({@code GrappleService}). Every character may do it (table ruling, 2026-10-07).
     */
    AGARRAR(null),

    /** Libertar-se do Agarrão — 2PA, a Perícia de Ataque against the captor's Ataque GD. */
    LIBERTAR_SE_DO_AGARRAO(ActionKind.ESCAPE_GRAPPLE),

    /**
     * The captor's roll when a held foe tries to escape — the player rolls their Ataque against the
     * foe's Ataque GD, and a failure frees it. Answers the foe's action rather than being one, so it
     * is {@link ActionKind#DEFENCE}-like and no Condição refuses it.
     */
    MANTER_AGARRAO(ActionKind.DEFENCE),

    /** Libertar-se da Imobilização — 2PA, Furtividade against a pre-set GD or the captor's Ataque GD. */
    LIBERTAR_SE_DA_IMOBILIZACAO(ActionKind.ESCAPE_IMMOBILIZATION),

    /** Libertar-se da Predação — an Ataque Corpo-a-Corpo from inside the devourer. */
    LIBERTAR_SE_DA_PREDACAO(ActionKind.ESCAPE_DEVOURED),

    /** Levantar-se — 1PA, leaving Caído; provokes Defender o Perímetro ({@code StandUpService}). */
    LEVANTAR_SE(ActionKind.STAND_UP);

    private final ActionKind actionKind;

    Manoeuvre(final ActionKind actionKind) {
        this.actionKind = actionKind;
    }

    /**
     * The {@link ActionKind} this manoeuvre is when a Condição asks — {@code null} when it is the
     * kind of Perícia roll it is made with (an Investida and an Agarrar are attacks).
     */
    public ActionKind getActionKind() {
        return actionKind;
    }
}
