package org.aventyrs.core.character.services;

import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.scene.grid.GridPosition;
import org.aventyrs.core.scene.grid.MovementMap;
import org.aventyrs.core.scene.grid.TerrainCell;
import org.aventyrs.core.scene.grid.TerrainKind;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.Condition;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.util.TranslatableMessages;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Postura Defensiva, Rendido, Imobilizar, Encontrão, the push and the charge line (core 0.1.6.19). */
class AttackManoeuvresTest {

    private static final SkillRoll HIGH = new SkillRoll(List.of(6, 6, 5));
    private static final SkillRoll LOW = new SkillRoll(List.of(1, 2, 2));

    private final DefenseService defenseService = new DefenseServiceImpl();
    private final DefensiveStanceService stance = new DefensiveStanceServiceImpl();
    private final ImmobilizeService immobilize = new ImmobilizeServiceImpl();
    private final ShoveChargeService shove = new ShoveChargeServiceImpl();
    private final ForcedMovementService push = new ForcedMovementServiceImpl();
    private final ChargeService charge = new ChargeServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static CharacterSheet sheet() {
        return CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK).id(UUID.randomUUID()).build(),
                new Player());
    }

    @Test
    void posturaDefensivaGivesTwoUntilTheHoldersNextTurn() {
        CharacterSheet sheet = sheet();
        int before = defenseService.getTotalDefense(sheet, DefenseType.PHYSICAL);

        stance.take(sheet, null);
        assertEquals(before + 2, defenseService.getTotalDefense(sheet, DefenseType.PHYSICAL));
        stance.take(sheet, null);
        assertEquals(before + 2, defenseService.getTotalDefense(sheet, DefenseType.PHYSICAL), "renewed, not stacked");

        sheet.startTurn(2);
        assertEquals(before, defenseService.getTotalDefense(sheet, DefenseType.PHYSICAL));
    }

    @Test
    void rendidoIsDesprevenidoAndEndsWhenItsHolderActs() {
        CharacterSheet sheet = sheet();
        sheet.applyCondition(new Condition(ConditionType.RENDIDO, null));

        assertTrue(sheet.hasCondition(ConditionType.DESPREVENIDO, null));
        sheet.recordAction(new org.aventyrs.core.sheet.CombatantAction(
                org.aventyrs.core.skill.SkillType.ATAQUE_CORPO_A_CORPO, null, null, null, 1, null, null, Set.of()));
        assertFalse(sheet.hasCondition(ConditionType.RENDIDO, null));
    }

    @Test
    void imobilizarNeedsARendidoOrUnconsciousAdjacentTarget() {
        CharacterSheet captor = sheet();
        CharacterSheet target = sheet();
        SceneContext adjacent = new SceneContext(List.of(), List.of(target), Map.of(target, Range.ADJACENTE));

        assertEquals(TranslatableMessages.IMMOBILIZE_TARGET_NOT_HELPLESS,
                immobilize.refusal(captor, target, adjacent).orElseThrow());
        target.applyCondition(new Condition(ConditionType.RENDIDO, null));
        immobilize.immobilize(captor, target, adjacent);

        assertTrue(target.hasCondition(ConditionType.IMOBILIZADO, null));
    }

    @Test
    void anEncontraoThatLandsPushesAndAMissCostsTheDefesas() {
        CharacterSheet attacker = sheet();
        CharacterSheet target = sheet();

        ShoveChargeService.ShoveResult landed = shove.shove(attacker, target, 5, null, HIGH);
        assertTrue(landed.hit());
        assertTrue(landed.pushUd() >= ShoveChargeService.PUSH_UD);
        assertTrue(landed.knockedDown(), "a wide margin carries Lançar ao Chão");

        CharacterSheet other = sheet();
        ShoveChargeService.ShoveResult missed = shove.shove(other, sheet(), 40, null, LOW);
        assertFalse(missed.hit());
        assertNotNull(missed.missPenalty());
    }

    @Test
    void aPushGoesStraightBackAndStopsAtAWallOrFallsOffALedge() {
        CharacterSheet target = sheet();
        GridPosition pusher = new GridPosition(5, 2);
        GridPosition at = new GridPosition(5, 3);
        MovementMap open = new MovementMap(20, 20, Set.of(), Map.of(), Set.of(), Map.of());
        assertEquals(new GridPosition(5, 5), push.push(target, pusher, at, 2, open).landing());

        MovementMap walled = new MovementMap(20, 20, Set.of(), Map.of(), Set.of(),
                Map.of(new GridPosition(5, 5), new TerrainCell(TerrainKind.OBSTACLE, false, 3, null)));
        ForcedMovementService.PushResult blocked = push.push(target, pusher, at, 2, walled);
        assertEquals(new GridPosition(5, 4), blocked.landing());
        assertTrue(blocked.stoppedShort());

        MovementMap cliff = new MovementMap(20, 20, Set.of(), Map.of(), Set.of(),
                Map.of(at, new TerrainCell(TerrainKind.GROUND, false, 6, null)));
        assertEquals(6, push.push(target, pusher, at, 1, cliff).fallUd());
    }

    @Test
    void aChargeLineMustBeLongEnoughAndClear() {
        CharacterSheet sheet = sheet();
        GridPosition from = new GridPosition(2, 2);
        MovementMap open = new MovementMap(20, 20, Set.of(), Map.of(), Set.of(), Map.of());
        int minimum = charge.getMinimumDistance(sheet);

        assertEquals(TranslatableMessages.CHARGE_TOO_SHORT,
                charge.refusalForLine(sheet, from, new GridPosition(2, 2 + minimum - 1), open).orElseThrow());
        assertTrue(charge.refusalForLine(sheet, from, new GridPosition(2, 2 + minimum), open).isEmpty());
        MovementMap blocked = new MovementMap(20, 20, Set.of(), Map.of(), Set.of(),
                Map.of(new GridPosition(2, 3), new TerrainCell(TerrainKind.OBSTACLE, false, 1, null)));
        assertEquals(TranslatableMessages.CHARGE_LINE_BLOCKED,
                charge.refusalForLine(sheet, from, new GridPosition(2, 2 + minimum), blocked).orElseThrow());
    }

    @Test
    void anObjectAttackNeedsTheItemEquipped() {
        CharacterSheet attacker = sheet();
        CharacterSheet target = sheet();
        org.aventyrs.core.item.Item stray = org.aventyrs.core.item.ArmorItem.ROBE_CERIMONIAL;

        assertThrows(IllegalOperationException.class, () -> new ObjectAttackServiceImpl().strike(attacker, target, 5,
                stray, org.aventyrs.core.skill.SkillType.ATAQUE_CORPO_A_CORPO, HIGH, 8, null));
    }
}
