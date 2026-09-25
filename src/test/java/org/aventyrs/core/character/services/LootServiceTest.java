package org.aventyrs.core.character.services;

import java.util.List;
import java.util.Map;

import org.aventyrs.core.character.CharacterStatus;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.aventyrs.core.util.TranslatableMessages.LOOT_TARGET_NOT_AN_ENEMY;
import static org.aventyrs.core.util.TranslatableMessages.LOOT_TARGET_NOT_DEFEATED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Saquear: the table's 2PA rule, gated on a defeated enemy. */
class LootServiceTest {

    private final LootService lootService = new LootServiceImpl();

    private CombatantSheet ally;
    private CombatantSheet foe;

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
        ally = sheet();
        foe = sheet();
    }

    private static CombatantSheet sheet() {
        return CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK).build(), new Player());
    }

    private SceneContext context(final boolean combat) {
        return new SceneContext(List.of(ally), List.of(foe), Map.of(), null, combat, 1, false);
    }

    @Test
    void lootingCostsTwoActionPointsInCombat() {
        ActionCost cost = lootService.requireLootable(context(true), foe, CharacterStatus.FALLEN);

        assertEquals(ActionCost.Kind.FIXED, cost.kind());
        assertEquals(2, cost.actionPoints());
        assertEquals(LootService.LOOT_COST, cost);
    }

    @Test
    void lootingOutsideCombatIsFree() {
        assertEquals(ActionCost.NONE, lootService.requireLootable(context(false), foe, CharacterStatus.DEAD));
        assertEquals(ActionCost.NONE, lootService.getLootCost(null));
    }

    @Test
    void onlyAFoeAtZeroPvOrBelowIsDefeated() {
        for (CharacterStatus status : CharacterStatus.values()) {
            boolean expected = status == CharacterStatus.FALLEN || status == CharacterStatus.COMMA
                    || status == CharacterStatus.DEAD;
            assertEquals(expected, LootService.isDefeated(status), status.name());
            assertEquals(expected, lootService.canLoot(context(true), foe, status), status.name());
        }
        assertFalse(LootService.isDefeated(null), "an unknown status is never defeated");
    }

    @Test
    void aStandingFoeIsRefused() {
        IllegalOperationException refused = assertThrows(IllegalOperationException.class,
                () -> lootService.requireLootable(context(true), foe, CharacterStatus.LOW_LIFE));

        assertEquals(LOOT_TARGET_NOT_DEFEATED, refused.getMessage());
    }

    /** A fallen ally is carried to safety, not stripped. */
    @Test
    void aFallenAllyIsRefused() {
        IllegalOperationException refused = assertThrows(IllegalOperationException.class,
                () -> lootService.requireLootable(context(true), ally, CharacterStatus.FALLEN));

        assertEquals(LOOT_TARGET_NOT_AN_ENEMY, refused.getMessage());
    }

    @Test
    void equippingCostsOneActionPointOnlyInCombat() {
        assertEquals(CharacterSheet.EQUIP_COST_IN_COMBAT, CharacterSheet.getEquipCost(context(true)));
        assertEquals(1, CharacterSheet.EQUIP_COST_IN_COMBAT.actionPoints());
        assertEquals(ActionCost.NONE, CharacterSheet.getEquipCost(context(false)));
        assertEquals(ActionCost.NONE, CharacterSheet.getEquipCost(null));
    }

    @Test
    void theEnemyCheckMatchesById() {
        assertTrue(lootService.canLoot(context(true), foe, CharacterStatus.COMMA));
        assertFalse(lootService.canLoot(context(true), sheet(), CharacterStatus.COMMA),
                "someone not in the Cena at all is nobody's enemy");
    }
}
