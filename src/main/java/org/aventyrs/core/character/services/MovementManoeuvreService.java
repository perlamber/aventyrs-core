package org.aventyrs.core.character.services;

import org.aventyrs.core.action.Manoeuvre;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.scene.grid.BoardPosition;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.skill.CriticalResult;
import org.aventyrs.core.skill.DifficultyLevel;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.IntUnaryOperator;

/**
 * The movements of "Ações de Combate – Movimentos" (acoes-de-combate.txt 20261013, core 0.1.6.18) beyond the plain
 * Movimento Padrão: Correr, Escalar, Nadar, Saltar, Recuo Defensivo and taking off (Voar). Reposicionar keeps its own
 * {@code RepositionService}.
 *
 * <p>Caller-driven like every geometry here. The caller rolls Atletismo (Tri-atleta) against the GD and reports the
 * total and critical result; this service turns it into how far the mover may go ({@link #resolve}), and judges a
 * Recuo's path ({@link #isRetreatPathAllowed}). Every one of them is a movement bought with PA, so it claims a movement
 * of the Rodada and excludes Reposicionar this Turn (table ruling); PA are reported, never deducted.
 *
 * <p>Readings (approved with the 2026-10-14 plan): "+2UD para cada GD superada" counts beating the starting Médio;
 * Nadar's exemption is a Natação Movimento Base (the text's "Vertical" is a typo).
 */
public interface MovementManoeuvreService {

    /** Correr, Escalar, Nadar, Saltar and Recuo Defensivo: "Tempo de Ação: 2PA". */
    ActionCost MANOEUVRE_COST = ActionCost.ofActionPoints(2);

    /** "iniciado em Médio". */
    DifficultyLevel STARTING_DIFFICULTY = DifficultyLevel.MEDIUM;

    /** "+2UD ao movimento para cada GD superada". */
    int UD_PER_LEVEL_BEATEN = 2;

    /** Escalar/Nadar: a failure by up to this margin is no movement; beyond it, a fall or submersion. */
    int SAFE_FAILURE_MARGIN = 5;

    /** What manoeuvre costs — 2PA for the five, nothing extra for taking off (its movement is priced as flight). */
    ActionCost getCost(Manoeuvre manoeuvre);

    /** Why sheet may not make manoeuvre now — a {@code TranslatableMessages} key — or empty. */
    Optional<String> refusal(CombatantSheet sheet, Manoeuvre manoeuvre, SceneContext sceneContext);

    /**
     * Whether manoeuvre needs the Atletismo roll for sheet: Correr and Saltar always, Escalar unless a Vertical
     * Movimento Base, Nadar unless a Natação one; Recuo and Voar never.
     */
    boolean requiresRoll(CombatantSheet sheet, Manoeuvre manoeuvre);

    /**
     * What the manoeuvre comes to.
     *
     * @param difficulty   the GD rolled against — {@link #STARTING_DIFFICULTY} for Correr/Saltar, the cell's for
     *                     Escalar/Nadar; ignored with no roll
     * @param rollTotal    the Atletismo total, {@code null} when no roll was needed
     * @param critical     the roll's critical result, or {@code null}
     * @param uniform      a caller-supplied random 1..n — for a stumble's distance; may be {@code null} when none
     *                     can occur
     */
    MovementOutcome resolve(CombatantSheet sheet, Manoeuvre manoeuvre, DifficultyLevel difficulty, Integer rollTotal,
                            CriticalResult critical, IntUnaryOperator uniform);

    /**
     * Recuo Defensivo's path (table ruling, 2026-10-14): no step may bring the mover closer to any enemy within
     * Distância Curta of the start, and the end must be farther from each of them than the start.
     */
    boolean isRetreatPathAllowed(BoardPosition start, List<BoardPosition> steps, Collection<BoardPosition> enemies);

    /**
     * Escalar/Nadar: "Se houver alguma ameaça ou personagem hostil, o personagem … recebe a Condição Desprevenido,
     * exceto quando ele tiver Movimento Base Vertical" (Natação for Nadar). threatened is the caller's word.
     */
    boolean isExposed(CombatantSheet sheet, Manoeuvre manoeuvre, boolean threatened);

    /**
     * What a movement manoeuvre came to.
     *
     * @param kind          how it ended
     * @param distanceUd    how far the mover may go (vertically for Escalar) — 0 when it does not move
     * @param stumbleUd     for {@link Kind#STUMBLES}, how far it gets before falling Caído
     * @param levelsBeaten  GD levels beaten from Médio up — what the +2UD counts
     */
    record MovementOutcome(Manoeuvre manoeuvre, Kind kind, int distanceUd, int stumbleUd, int levelsBeaten) {

        /** How it ended. */
        public enum Kind {
            /** Moves up to {@code distanceUd}. */
            MOVES,
            /** Escalar/Nadar failed by 5 or less: no movement. */
            NO_MOVE,
            /** Escalar failed by more than 5: falls — Danos de Queda and Caído. */
            FALLS,
            /** Nadar failed by more than 5: submerges — the asphyxia clock starts. */
            SUBMERGES,
            /** Correr/Saltar Falha Crítica: moves {@code stumbleUd}, then Caído. */
            STUMBLES
        }
    }
}
