package org.aventyrs.core.character.services;

import org.aventyrs.core.ego.InitiativeRollCharge;
import org.aventyrs.core.scene.Scene;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.skill.SkillRoll;

/**
 * Spending Iniciativa (2.5 Ego › Iniciativa; Ego plan Phase 7, core 0.0.79). Each method pays one point of the
 * type the rules text puts it under — a temporary point can never buy a permanent effect — through {@code
 * EgoPointsService#useEgoPointsForEffect}, and refuses with {@code NOT_ENOUGH_EGO_POINTS}, spending nothing, when
 * that point isn't there. No timing gate: Egos are spent at any time (table ruling).
 *
 * <p>The order effects are {@code CombatantSheet#overrideInitiative}: they replace the combatant's Iniciativa and
 * take hold at the next Rodada boundary, when the {@link Scene} re-sorts — the same point every mid-Scene
 * Iniciativa change already waits for.
 */
public interface InitiativeEgoService {

    /** How long an order change lasts — "por uma Cena ou Rodada". */
    enum Span {
        /** The next Rodada's order. */
        RODADA,
        /** The rest of the Cena. */
        CENA
    }

    /** "Alterar o valor de Iniciativa de um PdN por 2 Rodadas." */
    int OPPONENT_RODADAS = 2;

    /** "Adquirir +2PA e 1 Reação adicional por 2 Rodadas." */
    int SURGE_ACTION_POINTS = 2;
    int SURGE_REACTIONS = 1;
    int SURGE_RODADAS = 2;

    /**
     * Temporary: "reduzir voluntariamente seu valor de Iniciativa para qualquer valor menor por uma Cena ou
     * Rodada".
     *
     * @throws org.aventyrs.core.sheet.IllegalOperationException {@code INVALID_INITIATIVE_CHANGE} when sheet is
     *         not in scene or newValue is not lower than its current Iniciativa (nothing spent)
     */
    void lowerInitiative(Scene scene, CombatantSheet sheet, int newValue, Span span);

    /**
     * Temporary: "refazer sua rolagem de Iniciativa com Vantagem; aplicável apenas na Cena atual". newTotal is
     * the new 3d6 + Iniciativa the caller rolled; the Vantagem ({@code Skill#ADVANTAGE_BONUS}) is added here. It
     * stands for the rest of the Cena, whether higher or lower.
     *
     * @throws org.aventyrs.core.sheet.IllegalOperationException {@code INVALID_INITIATIVE_CHANGE} when sheet is
     *         not in scene
     */
    void rerollInitiative(Scene scene, CombatantSheet sheet, int newTotal);

    /** Temporary: "adquirir +1PA … nesta Rodada". */
    void gainActionPoint(CombatantSheet sheet);

    /** Temporary: "adquirir … Reação adicional nesta Rodada". */
    void gainReaction(CombatantSheet sheet);

    /**
     * Temporary ({@code ADVANTAGE}) or permanent ({@code DIFFICULTY_REDUCTION}): banks {@link
     * InitiativeRollCharge#USES_PER_POINT} uses on sheet, for this Cena.
     */
    void bankRollCharges(CombatantSheet sheet, InitiativeRollCharge charge);

    /**
     * Spends one banked use of charge on roll and returns the marked roll. Nothing is paid here — the point was,
     * when banking.
     *
     * @throws org.aventyrs.core.sheet.IllegalOperationException {@code NO_INITIATIVE_CHARGE_BANKED} with none left
     */
    SkillRoll useRollCharge(CombatantSheet sheet, SkillRoll roll, InitiativeRollCharge charge);

    /**
     * Permanent: "alterar seu valor de Iniciativa para qualquer valor à sua escolha por uma Cena ou Rodada".
     *
     * @throws org.aventyrs.core.sheet.IllegalOperationException {@code INVALID_INITIATIVE_CHANGE} when sheet is
     *         not in scene
     */
    void setInitiative(Scene scene, CombatantSheet sheet, int newValue, Span span);

    /**
     * Permanent: "alterar o valor de Iniciativa de um PdN por 2 Rodadas". payer spends; opponent's order changes.
     * The Narrador's table decides what counts as a PdN — "não é recomendada … em combates entre PJs".
     *
     * @throws org.aventyrs.core.sheet.IllegalOperationException {@code INVALID_INITIATIVE_CHANGE} when opponent is
     *         not in scene
     */
    void setOpponentInitiative(Scene scene, CombatantSheet payer, CombatantSheet opponent, int newValue);

    /** Permanent: "adquirir +2PA e 1 Reação adicional por 2 Rodadas". */
    void gainActionSurge(CombatantSheet sheet);
}
