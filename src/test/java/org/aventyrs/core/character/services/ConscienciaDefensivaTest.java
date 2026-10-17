package org.aventyrs.core.character.services;

import org.aventyrs.core.action.ActionProfile;
import org.aventyrs.core.action.Manoeuvre;
import org.aventyrs.core.action.MovementProgress;
import org.aventyrs.core.action.ReactionCheckpoint;
import org.aventyrs.core.character.DamageBase;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.item.AbstractWeapon;
import org.aventyrs.core.item.ItemCategory;
import org.aventyrs.core.item.ItemWeightClass;
import org.aventyrs.core.item.Weapon;
import org.aventyrs.core.monster.MonsterTemplate;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.scene.grid.BoardPosition;
import org.aventyrs.core.scene.grid.GridPosition;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.SkillType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link ActionProfile#CONSCIENCIA_DEFENSIVA} — "Em seu primeiro movimento de cada Rodada, nos 2 primeiros UD
 * percorridos, você não provoca Reações. Em Rodadas ímpares nenhum de seus movimentos provoca Reações." (core 0.1.7.9;
 * table rulings 2026-10-16: odd = the counter as shown, the window is hexes travelled, and a hostile still in reach
 * after the window reacts at its next covered hex).
 */
class ConscienciaDefensivaTest {

    private final MovementReactionService movement = new MovementReactionServiceImpl();

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

    private static CharacterSheet combatant(final ActionProfile profile, final boolean armed) {
        CharacterSheet sheet = CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK)
                .id(UUID.randomUUID()).actionProfile(profile).feats(new ArrayList<>()).equipment(new ArrayList<>())
                .drawnWeapons(new ArrayList<>()).build(), new Player());
        if (armed) {
            Weapon sword = sword();
            sheet.getCharacter().equip(sword);
            sheet.getCharacter().drawWeapon(sword);
        }
        return sheet;
    }

    private static CharacterSheet wary() {
        return combatant(ActionProfile.CONSCIENCIA_DEFENSIVA, false);
    }

    private static CharacterSheet foe() {
        return combatant(null, true);
    }

    private static BoardPosition at(final int x, final int y) {
        return BoardPosition.at(new GridPosition(x, y));
    }

    /** A straight walk down column 5, from row 0 to row lastRow. */
    private static List<BoardPosition> walk(final int lastRow) {
        List<BoardPosition> route = new ArrayList<>();
        for (int y = 0; y <= lastRow; y++) {
            route.add(at(5, y));
        }
        return route;
    }

    private static SceneContext round(final CombatantSheet foe, final int round) {
        return new SceneContext(List.of(), List.of(foe), Map.of(foe, Range.ADJACENTE), null, true, round, false);
    }

    private List<ReactionCheckpoint> route(final CombatantSheet mover, final List<BoardPosition> route,
                                           final SceneContext context, final CombatantSheet foe,
                                           final BoardPosition foeAt, final MovementProgress progress) {
        return movement.getRouteCheckpoints(mover, route, null, context, Map.of(foe, foeAt), null, Map.of(), progress);
    }

    // ---------- odd Rodadas ----------

    @Test
    void noMovementProvokesOnAnOddRodada() {
        CharacterSheet foe = foe();

        assertTrue(route(wary(), walk(6), round(foe, 1), foe, at(5, 1), null).isEmpty());
        assertTrue(route(wary(), walk(6), round(foe, 3), foe, at(5, 4), MovementProgress.startOf(2)).isEmpty());
        assertTrue(movement.getProvokedReactors(wary(), round(foe, 1), Manoeuvre.INVESTIDA).isEmpty());
        assertTrue(movement.getProvokedReactors(wary(), round(foe, 3), Manoeuvre.CORRER).isEmpty());
    }

    @Test
    void theOpeningRodadaZeroAndEvenRodadasAreNotOdd() {
        CharacterSheet foe = foe();

        assertEquals(List.of(foe), movement.getProvokedReactors(wary(), round(foe, 0), null));
        assertEquals(List.of(foe), movement.getProvokedReactors(wary(), round(foe, 2), null));
    }

    @Test
    void outsideACenaDeCombateNoRodadaIsOdd() {
        CharacterSheet foe = foe();
        SceneContext calm = new SceneContext(List.of(), List.of(foe), Map.of(foe, Range.ADJACENTE), null, false, 1,
                false);

        assertEquals(List.of(foe), movement.getProvokedReactors(wary(), calm, null));
    }

    @Test
    void levantarSeIsNotAMovementAndStillProvokesOnAnOddRodada() {
        CharacterSheet foe = foe();

        assertEquals(List.of(foe), movement.getProvokedReactors(wary(), round(foe, 1), Manoeuvre.LEVANTAR_SE));
    }

    // ---------- the first movement's first 2UD ----------

    /** A foe whose reach covers indices 0–2 reacts at index 2, as the mover takes its third step. */
    @Test
    void aFoeStillInReachAfterTheWindowReactsAtTheNextCoveredHex() {
        CharacterSheet foe = foe();

        List<ReactionCheckpoint> checkpoints = route(wary(), walk(6), round(foe, 2), foe, at(5, 1),
                MovementProgress.startOf(0));

        assertEquals(1, checkpoints.size());
        assertEquals(2, checkpoints.get(0).routeIndex());
        assertEquals(List.of(foe), checkpoints.get(0).reactors());
    }

    /** A foe whose reach covers only indices 0–1 never sees the mover leave its reach outside the window. */
    @Test
    void aFoeCoveringOnlyTheWindowDoesNotReact() {
        CharacterSheet foe = foe();

        assertTrue(route(wary(), walk(6), round(foe, 2), foe, at(5, 0), MovementProgress.startOf(0)).isEmpty());
    }

    @Test
    void aRouteNoLongerThanTheWindowProvokesNothing() {
        CharacterSheet foe = foe();

        assertTrue(route(wary(), walk(2), round(foe, 2), foe, at(5, 1), MovementProgress.startOf(0)).isEmpty());
        assertEquals(1, route(wary(), walk(3), round(foe, 2), foe, at(5, 1), MovementProgress.startOf(0)).size());
    }

    @Test
    void theWindowShrinksByWhatTheMovementAlreadyTravelled() {
        CharacterSheet foe = foe();

        List<ReactionCheckpoint> oneLeft = route(wary(), walk(6), round(foe, 2), foe, at(5, 0),
                new MovementProgress(0, 1));
        List<ReactionCheckpoint> closed = route(wary(), walk(6), round(foe, 2), foe, at(5, 0),
                new MovementProgress(0, 2));

        assertEquals(1, oneLeft.get(0).routeIndex());
        assertEquals(0, closed.get(0).routeIndex());
    }

    @Test
    void onlyTheFirstMovementOfTheRodadaHasAWindow() {
        CharacterSheet foe = foe();

        List<ReactionCheckpoint> second = route(wary(), walk(6), round(foe, 2), foe, at(5, 1),
                MovementProgress.startOf(1));

        assertEquals(0, second.get(0).routeIndex());
    }

    @Test
    void noProgressToldOpensNoWindow() {
        CharacterSheet foe = foe();

        assertEquals(0, route(wary(), walk(6), round(foe, 2), foe, at(5, 1), null).get(0).routeIndex());
    }

    @Test
    void anotherProfileHasNoWindow() {
        CharacterSheet foe = foe();
        CharacterSheet hasty = combatant(ActionProfile.IMPULSIVO, false);

        assertEquals(0, route(hasty, walk(6), round(foe, 2), foe, at(5, 1), MovementProgress.startOf(0)).get(0)
                .routeIndex());
        assertEquals(1, route(hasty, walk(6), round(foe, 1), foe, at(5, 1), null).size());
    }

    @Test
    void anInvestidaAsTheFirstMovementStartsInsideTheWindow() {
        CharacterSheet foe = foe();
        CharacterSheet charger = combatant(ActionProfile.CONSCIENCIA_DEFENSIVA, true);
        CharacterSheet target = foe();
        SceneContext context = new SceneContext(List.of(), List.of(foe, target),
                Map.of(foe, Range.ADJACENTE, target, Range.ADJACENTE), null, true, 2, false);
        ChargeService charges = new ChargeServiceImpl();
        Weapon weapon = charger.getCharacter().getDrawnWeapons().get(0);

        assertEquals(List.of(), charges.begin(charger, weapon, context, target).provokedReactors());
        assertEquals(List.of(foe), charges.begin(charger, weapon, context, target).provokedReactors(),
                "the second movement of the Rodada has no window");
    }

    // ---------- what is not a movement ----------

    @Test
    void castingInReachIgnoresEveryMovementExemption() {
        CharacterSheet foe = foe();

        assertEquals(List.of(foe), movement.getHostilesInReach(wary(), at(5, 0), round(foe, 1),
                Map.of(foe, at(5, 1)), Map.of()));
    }

    @Test
    void aFoeWithNoStatedProfileHasNoneAndProvokesLikeAnyone() {
        CharacterSheet foe = foe();

        assertNull(MonsterTemplate.DEFAULT_ACTION_PROFILE);
        assertEquals(1, route(combatant(null, false), walk(6), round(foe, 1), foe, at(5, 1),
                MovementProgress.startOf(0)).size());
    }
}
