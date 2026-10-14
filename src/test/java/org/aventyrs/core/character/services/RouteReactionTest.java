package org.aventyrs.core.character.services;

import org.aventyrs.core.action.GenericReaction;
import org.aventyrs.core.action.GenericReactionOption;
import org.aventyrs.core.action.Manoeuvre;
import org.aventyrs.core.action.ReactionCheckpoint;
import org.aventyrs.core.action.ReactionContext;
import org.aventyrs.core.action.ReactionTrigger;
import org.aventyrs.core.character.DamageBase;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.item.AbstractWeapon;
import org.aventyrs.core.item.ItemCategory;
import org.aventyrs.core.item.ItemWeightClass;
import org.aventyrs.core.item.Weapon;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.scene.grid.BoardPosition;
import org.aventyrs.core.scene.grid.GridPosition;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.sheet.PreparedAction;
import org.aventyrs.core.sheet.PreparedActionKind;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.util.TranslatableMessages;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Defender o Perímetro along a route and the Reações every character has (core 0.1.6.17; table ruling 2026-10-14). */
class RouteReactionTest {

    private final MovementReactionService movement = new MovementReactionServiceImpl();
    private final ReactionOptionsService options = new ReactionOptionsServiceImpl();
    private final PreparationService preparation = new PreparationServiceImpl();
    private final SacrificeService sacrifice = new SacrificeServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static Weapon sword() {
        return AbstractWeapon.builder()
                .name("Espada").category(ItemCategory.LIGHT_BLADE).weightClass(ItemWeightClass.LIGHT)
                .damageBase(DamageBase.of(1, 2)).skillType(SkillType.ATAQUE_CORPO_A_CORPO)
                .build();
    }

    private static CharacterSheet combatant(final boolean armed) {
        CharacterSheet sheet = CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK)
                .id(UUID.randomUUID()).feats(new ArrayList<>()).equipment(new ArrayList<>())
                .drawnWeapons(new ArrayList<>()).build(), new Player());
        if (armed) {
            Weapon sword = sword();
            sheet.getCharacter().equip(sword);
            sheet.getCharacter().drawWeapon(sword);
        }
        return sheet;
    }

    private static BoardPosition at(final int x, final int y) {
        return BoardPosition.at(new GridPosition(x, y));
    }

    /** A straight walk down column 5, from row 0 to row 6. */
    private static List<BoardPosition> walk() {
        List<BoardPosition> route = new ArrayList<>();
        for (int y = 0; y <= 6; y++) {
            route.add(at(5, y));
        }
        return route;
    }

    @Test
    void eachFoeReactsOnceWhereItsReachFirstCoversTheRoute() {
        CharacterSheet mover = combatant(false);
        CharacterSheet first = combatant(true);
        CharacterSheet second = combatant(true);
        SceneContext context = new SceneContext(List.of(), List.of(first, second),
                Map.of(first, Range.DISTANCIA_CURTA, second, Range.DISTANCIA_CURTA));

        List<ReactionCheckpoint> checkpoints = movement.getRouteCheckpoints(mover, walk(), null, context,
                Map.of(first, at(6, 2), second, at(4, 5)), null);

        assertEquals(2, checkpoints.size());
        assertEquals(List.of(first), checkpoints.get(0).reactors());
        assertEquals(List.of(second), checkpoints.get(1).reactors());
        assertTrue(checkpoints.get(0).routeIndex() < checkpoints.get(1).routeIndex());
    }

    @Test
    void leavingAThreatenedStartProvokesAtIndexZero() {
        CharacterSheet mover = combatant(false);
        CharacterSheet foe = combatant(true);
        SceneContext context = new SceneContext(List.of(), List.of(foe), Map.of(foe, Range.ADJACENTE));

        List<ReactionCheckpoint> checkpoints = movement.getRouteCheckpoints(mover, walk(), null, context,
                Map.of(foe, at(5, 1)), null);

        assertEquals(0, checkpoints.get(0).routeIndex());
    }

    @Test
    void anUnarmedFoeAReposicionarAndAFlyerOutOfReachProvokeNothing() {
        CharacterSheet mover = combatant(false);
        CharacterSheet unarmed = combatant(false);
        CharacterSheet foe = combatant(true);
        SceneContext context = new SceneContext(List.of(), List.of(unarmed, foe),
                Map.of(unarmed, Range.ADJACENTE, foe, Range.ADJACENTE));
        Map<CombatantSheet, BoardPosition> positions = Map.of(unarmed, at(5, 1), foe, at(6, 2));

        assertEquals(List.of(foe), movement.getRouteCheckpoints(mover, walk(), null, context, positions, null)
                .get(0).reactors());
        assertTrue(movement.getRouteCheckpoints(mover, walk(), Manoeuvre.REPOSICIONAR, context, positions, null)
                .isEmpty());
        List<BoardPosition> high = walk().stream().map(step -> new BoardPosition(step.hex(), 4)).toList();
        assertTrue(movement.getRouteCheckpoints(mover, high, null, context, positions, null).isEmpty());
    }

    @Test
    void anInvestidasTargetDoesNotReact() {
        CharacterSheet mover = combatant(false);
        CharacterSheet target = combatant(true);
        SceneContext context = new SceneContext(List.of(), List.of(target), Map.of(target, Range.ADJACENTE));

        assertTrue(movement.getRouteCheckpoints(mover, walk(), Manoeuvre.INVESTIDA, context,
                Map.of(target, at(5, 7)), target).isEmpty());
    }

    @Test
    void defenderOPerimetroIsOfferedToAnArmedReactor() {
        CharacterSheet reactor = combatant(true);
        CharacterSheet mover = combatant(false);
        ReactionContext context = ReactionContext.builder().trigger(ReactionTrigger.ENEMY_MOVES_IN_REACH)
                .reactor(reactor).attacker(mover)
                .reactorContext(new SceneContext(List.of(), List.of(mover), Map.of(mover, Range.ADJACENTE)))
                .build();

        List<GenericReactionOption> offered = options.getGenericReactions(context);

        assertTrue(offered.stream().anyMatch(option -> option.reaction() == GenericReaction.DEFENDER_O_PERIMETRO
                && option.affordable()));
        assertFalse(offered.stream().anyMatch(option -> option.reaction() == GenericReaction.IMPEDIR_CONJURACAO));
    }

    @Test
    void impedirConjuracaoNeedsTheCasterWithinReach() {
        CharacterSheet reactor = combatant(true);
        CharacterSheet caster = combatant(false);
        ReactionContext near = ReactionContext.builder().trigger(ReactionTrigger.ENEMY_CASTS_IN_REACH)
                .reactor(reactor).attacker(caster)
                .reactorContext(new SceneContext(List.of(), List.of(caster), Map.of(caster, Range.ADJACENTE)))
                .build();
        ReactionContext far = ReactionContext.builder().trigger(ReactionTrigger.ENEMY_CASTS_IN_REACH)
                .reactor(reactor).attacker(caster)
                .reactorContext(new SceneContext(List.of(), List.of(caster), Map.of(caster, Range.DISTANCIA_MEDIA)))
                .build();

        assertTrue(options.getGenericReactions(near).stream()
                .anyMatch(option -> option.reaction() == GenericReaction.IMPEDIR_CONJURACAO));
        assertTrue(options.getGenericReactions(far).isEmpty());
    }

    /** "-2 em sua Rolagem de Conjuração para cada 5 pontos de danos sofridos" — after mitigation (table ruling). */
    @Test
    void theDisruptionPenaltyIsTwoPerFivePoints() {
        assertEquals(0, org.aventyrs.core.magic.SpellCastingService.disruptionPenalty(4));
        assertEquals(-4, org.aventyrs.core.magic.SpellCastingService.disruptionPenalty(12));
    }

    @Test
    void sacrificioIsOncePerCombatAndNeedsTheAllyAdjacent() {
        CharacterSheet interceptor = combatant(false);
        CharacterSheet ally = combatant(false);
        SceneContext adjacent = new SceneContext(List.of(ally), List.of(), Map.of(ally, Range.ADJACENTE));
        SceneContext apart = new SceneContext(List.of(ally), List.of(), Map.of(ally, Range.DISTANCIA_CURTA));

        assertEquals(TranslatableMessages.SACRIFICE_ALLY_NOT_ADJACENT,
                sacrifice.refusalToSacrifice(interceptor, ally, apart, 1).orElseThrow());
        sacrifice.sacrifice(interceptor, ally, adjacent, 1);
        interceptor.startNewRound();

        assertEquals(TranslatableMessages.SACRIFICE_ALREADY_USED,
                assertThrows(IllegalOperationException.class,
                        () -> sacrifice.sacrifice(interceptor, ally, adjacent, 2)).getMessage());
    }

    @Test
    void aPreparacaoFiresOnceAndLapsesAtTheHoldersNextTurn() {
        CharacterSheet sheet = combatant(true);
        assertEquals(TranslatableMessages.PREPARED_ACTION_TOO_COSTLY,
                assertThrows(IllegalOperationException.class, () -> preparation.prepare(sheet,
                        PreparedActionKind.SPELL, "BOLA_DE_FOGO", "Bola de Fogo", ActionCost.ofActionPoints(3), null))
                        .getMessage());

        PreparedAction held = preparation.prepare(sheet, PreparedActionKind.ATTACK, "espada", "Ataque com a espada",
                ActionCost.ofActionPoints(2), null);
        assertEquals(PreparedActionKind.ATTACK, held.getKind());
        assertEquals("espada", held.getKey());
        assertTrue(preparation.canFire(sheet, null));
        assertSame(held, preparation.fire(sheet, null));
        assertFalse(preparation.canFire(sheet, null));

        preparation.prepare(sheet, PreparedActionKind.ATTACK, "espada", "Ataque com a espada",
                ActionCost.ofActionPoints(2), null);
        sheet.startTurn(2);
        assertFalse(preparation.canFire(sheet, null), "lapses as the holder's next Turn begins");
    }

    @Test
    void aReactionOrPassiveIsNotPreparableButAFreeActionIs() {
        CharacterSheet sheet = combatant(true);
        assertEquals(Optional.of(TranslatableMessages.PREPARED_ACTION_NOT_PREPARABLE),
                preparation.refusalToPrepare(sheet, ActionCost.REACTION, null));
        assertEquals(Optional.of(TranslatableMessages.PREPARED_ACTION_NOT_PREPARABLE),
                preparation.refusalToPrepare(sheet, ActionCost.NONE, null));
        assertTrue(preparation.refusalToPrepare(sheet, ActionCost.FREE_ACTION, null).isEmpty());
        assertTrue(preparation.refusalToPrepare(sheet, ActionCost.ofActionPoints(1), null).isEmpty());
    }

    /** A client's stand-in carries no weapon; its owner's advertised reach is what threatens. */
    @Test
    void aKnownReachStandsInForAnUnarmedStandIn() {
        CharacterSheet mover = combatant(false);
        CharacterSheet standIn = combatant(false);
        SceneContext context = new SceneContext(List.of(), List.of(standIn), Map.of(standIn, Range.DISTANCIA_CURTA));

        assertTrue(movement.getRouteCheckpoints(mover, walk(), null, context, Map.of(standIn, at(7, 3)), null)
                .isEmpty());
        assertEquals(1, movement.getRouteCheckpoints(mover, walk(), null, context, Map.of(standIn, at(7, 3)), null,
                Map.of(standIn, 2)).size());
        assertTrue(movement.getMeleeReachUd(combatant(true)).isPresent());
        assertTrue(movement.getMeleeReachUd(standIn).isEmpty());
    }
}
