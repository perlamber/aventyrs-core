package org.aventyrs.core.character.services;

import org.aventyrs.core.action.Manoeuvre;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.scene.grid.BoardPosition;
import org.aventyrs.core.scene.grid.GridPosition;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.CriticalResult;
import org.aventyrs.core.skill.DifficultyLevel;
import org.aventyrs.core.util.TranslatableMessages;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.aventyrs.core.character.services.MovementManoeuvreService.MovementOutcome.Kind;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Correr, Escalar, Nadar, Saltar, Recuo Defensivo and Voar (acoes-de-combate.txt 20261013, core 0.1.6.18). */
class MovementManoeuvreServiceTest {

    private final MovementManoeuvreService service = new MovementManoeuvreServiceImpl();
    private final MovementService movementService = new MovementServiceImpl();
    private CharacterSheet sheet;
    private int base;

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        sheet = CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK).id(UUID.randomUUID()).build(),
                new Player());
        base = movementService.getMovementBase(sheet);
    }

    @Test
    void correrIsTwiceTheBasePlusTwoPerLevelBeaten() {
        assertEquals(2 * base, service.resolve(sheet, Manoeuvre.CORRER, null, 10, null, null).distanceUd(),
                "a failure costs nothing but the bonus");
        int medio = DifficultyLevel.MEDIUM.getBaseValue();
        int dificil = DifficultyLevel.HARD.getBaseValue();
        assertEquals(2 * base + 2, service.resolve(sheet, Manoeuvre.CORRER, null, medio, null, null).distanceUd());
        assertEquals(2 * base + 4, service.resolve(sheet, Manoeuvre.CORRER, null, dificil, null, null).distanceUd());
    }

    @Test
    void aFalhaCriticaStumblesThenFalls() {
        var outcome = service.resolve(sheet, Manoeuvre.CORRER, null, 3, CriticalResult.FALHA_CRITICA_MAIOR,
                n -> n);
        assertEquals(Kind.STUMBLES, outcome.kind());
        assertEquals(base, outcome.stumbleUd());
    }

    @Test
    void saltarIsHalfTheBasePlusTwoPerLevel() {
        int half = Math.max(1, base / 2);
        assertEquals(half + 2, service.resolve(sheet, Manoeuvre.SALTAR, null,
                DifficultyLevel.MEDIUM.getBaseValue(), null, null).distanceUd());
    }

    @Test
    void escalarFailingByFiveStaysByMoreFalls() {
        int gd = DifficultyLevel.MEDIUM.getBaseValue();
        assertEquals(Kind.MOVES, service.resolve(sheet, Manoeuvre.ESCALAR, DifficultyLevel.MEDIUM, gd, null, null).kind());
        assertEquals(Kind.NO_MOVE, service.resolve(sheet, Manoeuvre.ESCALAR, DifficultyLevel.MEDIUM, gd - 5, null, null)
                .kind());
        assertEquals(Kind.FALLS, service.resolve(sheet, Manoeuvre.ESCALAR, DifficultyLevel.MEDIUM, gd - 6, null, null)
                .kind());
        assertEquals(Kind.SUBMERGES, service.resolve(sheet, Manoeuvre.NADAR, DifficultyLevel.MEDIUM, gd - 6, null, null)
                .kind());
    }

    @Test
    void climbingAndSwimmingWithoutTheModeNeedTheRollAndExpose() {
        assertTrue(service.requiresRoll(sheet, Manoeuvre.ESCALAR));
        assertTrue(service.isExposed(sheet, Manoeuvre.NADAR, true));
        assertFalse(service.isExposed(sheet, Manoeuvre.NADAR, false));
        assertFalse(service.requiresRoll(sheet, Manoeuvre.RECUO_DEFENSIVO));
    }

    @Test
    void recuoIsOncePerRodadaAndExcludesReposicionar() {
        sheet.consumeRetreatThisRound();
        assertEquals(TranslatableMessages.RETREAT_ALREADY_TAKEN,
                service.refusal(sheet, Manoeuvre.RECUO_DEFENSIVO, null).orElseThrow());
        sheet.startTurn(2);
        assertTrue(service.refusal(sheet, Manoeuvre.RECUO_DEFENSIVO, null).isEmpty());
        sheet.consumeRepositionThisRound();
        assertEquals(TranslatableMessages.MOVEMENT_AFTER_REPOSITION,
                service.refusal(sheet, Manoeuvre.CORRER, null).orElseThrow());
    }

    @Test
    void aGroundboundCombatantCannotTakeOff() {
        assertEquals(TranslatableMessages.CANNOT_FLY, service.refusal(sheet, Manoeuvre.VOAR, null).orElseThrow());
    }

    /** Table ruling (2026-10-14): never a step closer to a foe within Curta, and the end farther than the start. */
    @Test
    void aRecuoMustAlwaysMoveAway() {
        BoardPosition start = BoardPosition.at(new GridPosition(5, 5));
        BoardPosition foe = BoardPosition.at(new GridPosition(5, 6));
        List<BoardPosition> away = List.of(BoardPosition.at(new GridPosition(5, 4)),
                BoardPosition.at(new GridPosition(5, 3)));
        List<BoardPosition> around = List.of(BoardPosition.at(new GridPosition(4, 5)),
                BoardPosition.at(new GridPosition(4, 6)));

        assertTrue(service.isRetreatPathAllowed(start, away, List.of(foe)));
        assertFalse(service.isRetreatPathAllowed(start, around, List.of(foe)));
    }

    /** "metade da Categoria de Tamanho do Personagem (mínimo 1UD)". */
    @Test
    void reposicionarIsHalfTheCategoryAtLeastOne() {
        assertEquals(1, RepositionServiceImpl.getBaseDistance(sheet));
    }
}
