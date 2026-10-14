package org.aventyrs.core.character.services;

import lombok.NonNull;
import org.aventyrs.core.action.Manoeuvre;
import org.aventyrs.core.character.MovementMode;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.scene.grid.BoardPosition;
import org.aventyrs.core.scene.grid.HexGrid;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.skill.CriticalResult;
import org.aventyrs.core.skill.DifficultyLevel;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.IntUnaryOperator;

import static org.aventyrs.core.util.TranslatableMessages.CANNOT_FLY;
import static org.aventyrs.core.util.TranslatableMessages.MOVEMENT_AFTER_REPOSITION;
import static org.aventyrs.core.util.TranslatableMessages.MOVEMENT_PREVENTED;
import static org.aventyrs.core.util.TranslatableMessages.RETREAT_ALREADY_TAKEN;
import static org.aventyrs.core.util.TranslatableMessages.RUN_IN_DIFFICULT_TERRAIN;

public class MovementManoeuvreServiceImpl implements MovementManoeuvreService {

    private final MovementService movementService;

    public MovementManoeuvreServiceImpl() {
        this(new MovementServiceImpl());
    }

    public MovementManoeuvreServiceImpl(final MovementService movementService) {
        this.movementService = movementService;
    }

    @Override
    public ActionCost getCost(@NonNull final Manoeuvre manoeuvre) {
        return manoeuvre == Manoeuvre.VOAR ? ActionCost.NONE : MANOEUVRE_COST;
    }

    @Override
    public Optional<String> refusal(@NonNull final CombatantSheet sheet, @NonNull final Manoeuvre manoeuvre,
                                    final SceneContext sceneContext) {
        if (sheet.isMovementPrevented(sceneContext)) {
            return Optional.of(MOVEMENT_PREVENTED);
        }
        if (sheet.getRepositionsTakenThisRound() > 0) {
            return Optional.of(MOVEMENT_AFTER_REPOSITION);
        }
        if (manoeuvre == Manoeuvre.CORRER && sceneContext != null
                && sceneContext.getEnvironmentalState().inDifficultTerrain()) {
            return Optional.of(RUN_IN_DIFFICULT_TERRAIN);
        }
        if (manoeuvre == Manoeuvre.RECUO_DEFENSIVO && sheet.getRetreatsTakenThisRound() > 0) {
            return Optional.of(RETREAT_ALREADY_TAKEN);
        }
        if (manoeuvre == Manoeuvre.VOAR && !movementService.hasMovementMode(sheet, MovementMode.FLIGHT)
                && !sheet.isFlying()) {
            return Optional.of(CANNOT_FLY);
        }
        return Optional.empty();
    }

    @Override
    public boolean requiresRoll(@NonNull final CombatantSheet sheet, @NonNull final Manoeuvre manoeuvre) {
        return switch (manoeuvre) {
            case CORRER, SALTAR -> true;
            case ESCALAR -> !movementService.hasMovementMode(sheet, MovementMode.CLIMB);
            case NADAR -> !movementService.hasMovementMode(sheet, MovementMode.SWIM);
            default -> false;
        };
    }

    @Override
    public MovementOutcome resolve(@NonNull final CombatantSheet sheet, @NonNull final Manoeuvre manoeuvre,
                                   final DifficultyLevel difficulty, final Integer rollTotal,
                                   final CriticalResult critical, final IntUnaryOperator uniform) {
        int base = movementService.getMovementBase(sheet);
        int half = Math.max(1, base / 2);
        boolean rolled = requiresRoll(sheet, manoeuvre) && rollTotal != null;
        boolean criticalFailure = rolled && critical != null && critical.isCriticalFailure();
        DifficultyLevel against = difficulty == null ? STARTING_DIFFICULTY : difficulty;
        return switch (manoeuvre) {
            case CORRER -> sprint(manoeuvre, 2 * base, base, rolled ? rollTotal : null, criticalFailure, uniform);
            case SALTAR -> sprint(manoeuvre, half, half, rolled ? rollTotal : null, criticalFailure, uniform);
            case ESCALAR, NADAR -> {
                if (!requiresRoll(sheet, manoeuvre)) {
                    yield new MovementOutcome(manoeuvre, MovementOutcome.Kind.MOVES, half, 0, 0);
                }
                int margin = against.getBaseValue() - (rollTotal == null ? 0 : rollTotal);
                if (margin <= 0) {
                    yield new MovementOutcome(manoeuvre, MovementOutcome.Kind.MOVES, half, 0, 0);
                }
                yield margin <= SAFE_FAILURE_MARGIN
                        ? new MovementOutcome(manoeuvre, MovementOutcome.Kind.NO_MOVE, 0, 0, 0)
                        : new MovementOutcome(manoeuvre, manoeuvre == Manoeuvre.ESCALAR
                                ? MovementOutcome.Kind.FALLS : MovementOutcome.Kind.SUBMERGES, 0, 0, 0);
            }
            case RECUO_DEFENSIVO -> new MovementOutcome(manoeuvre, MovementOutcome.Kind.MOVES, half, 0, 0);
            default -> new MovementOutcome(manoeuvre, MovementOutcome.Kind.MOVES, base, 0, 0);
        };
    }

    /** Correr and Saltar: a base distance, +2UD per GD beaten from Médio; a Falha Crítica stumbles. */
    private static MovementOutcome sprint(final Manoeuvre manoeuvre, final int distance, final int stumbleRange,
                                          final Integer rollTotal, final boolean criticalFailure,
                                          final IntUnaryOperator uniform) {
        if (criticalFailure) {
            int stumble = uniform == null ? 1 : Math.max(1, Math.min(stumbleRange, uniform.applyAsInt(stumbleRange)));
            return new MovementOutcome(manoeuvre, MovementOutcome.Kind.STUMBLES, 0, stumble, 0);
        }
        int levels = rollTotal == null ? 0 : levelsBeaten(rollTotal);
        return new MovementOutcome(manoeuvre, MovementOutcome.Kind.MOVES, distance + UD_PER_LEVEL_BEATEN * levels, 0,
                levels);
    }

    /** GD levels from Médio up whose value the total reaches — Médio beaten is 1. */
    static int levelsBeaten(final int total) {
        int levels = 0;
        for (DifficultyLevel level : DifficultyLevel.values()) {
            if (level.ordinal() >= STARTING_DIFFICULTY.ordinal() && total >= level.getBaseValue()) {
                levels++;
            }
        }
        return levels;
    }

    @Override
    public boolean isRetreatPathAllowed(@NonNull final BoardPosition start, @NonNull final List<BoardPosition> steps,
                                        @NonNull final Collection<BoardPosition> enemies) {
        if (steps.isEmpty()) {
            return false;
        }
        List<BoardPosition> near = enemies.stream()
                .filter(enemy -> Range.fromUnidadesDeDistancia(HexGrid.distance3d(start, enemy))
                        .isWithin(Range.DISTANCIA_CURTA))
                .toList();
        BoardPosition previous = start;
        for (BoardPosition step : steps) {
            for (BoardPosition enemy : near) {
                if (HexGrid.distance3d(step, enemy) < HexGrid.distance3d(previous, enemy)) {
                    return false;
                }
            }
            previous = step;
        }
        BoardPosition end = steps.get(steps.size() - 1);
        return near.stream().allMatch(enemy -> HexGrid.distance3d(end, enemy) > HexGrid.distance3d(start, enemy));
    }

    @Override
    public boolean isExposed(@NonNull final CombatantSheet sheet, @NonNull final Manoeuvre manoeuvre,
                             final boolean threatened) {
        if (!threatened) {
            return false;
        }
        return manoeuvre == Manoeuvre.ESCALAR && !movementService.hasMovementMode(sheet, MovementMode.CLIMB)
                || manoeuvre == Manoeuvre.NADAR && !movementService.hasMovementMode(sheet, MovementMode.SWIM);
    }
}
