package org.aventyrs.core.scene;

import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.aventyrs.core.util.TranslatableMessages.SCENE_NOT_IN_COMBAT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** {@link Scene#endCombat()} and the combat-scoped state it drops from each participant's sheet. */
class SceneEndCombatTest {

    private static final Object SOURCE = "a combat-scoped trait";

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private static CharacterSheet sheet() {
        return CharacterSheet.of(CharacterFixture.blank(CharacterFixture.BLANK).build(), new Player());
    }

    @Test
    void endingCombatTurnsTheFlagOffAndPutsTheRodadaBackToZero() {
        Scene scene = new Scene();
        scene.addParticipant(sheet(), 10);
        scene.startCombat();
        scene.next();
        scene.next();

        scene.endCombat();

        assertFalse(scene.isCombatScene());
        assertEquals(0, scene.getCurrentRound());
    }

    @Test
    void aSceneNotInCombatCannotEndOne() {
        IllegalOperationException refused = assertThrows(IllegalOperationException.class, () -> new Scene().endCombat());

        assertEquals(SCENE_NOT_IN_COMBAT, refused.getMessage());
    }

    @Test
    void combatScopedStateLapsesAndPlainBudgetsSurvive() {
        CharacterSheet sheet = sheet();
        CharacterSheet target = sheet();
        Scene scene = new Scene();
        scene.addParticipant(sheet, 10);
        scene.startCombat();
        sheet.incrementCombatCounter(SOURCE);
        sheet.markAffectedThisCombat(SOURCE);
        sheet.grantEnhancedAttacksForCombat(SOURCE, 3);
        sheet.grantEnhancedAttacks("a budget with no end", 2);
        sheet.recordNaturalWeaponHit(target, 0);
        sheet.recordNaturalWeaponHit(target, 1);
        assertTrue(sheet.hasHitWithNaturalWeaponInConsecutiveRounds(target, 1));

        scene.endCombat();

        assertEquals(0, sheet.getCombatCounter(SOURCE));
        assertFalse(sheet.isAffectedThisCombat(SOURCE));
        assertEquals(0, sheet.getRemainingEnhancedAttacks(SOURCE));
        assertEquals(2, sheet.getRemainingEnhancedAttacks("a budget with no end"));
        assertFalse(sheet.hasHitWithNaturalWeaponInConsecutiveRounds(target, 1));
    }

    @Test
    void aPlainGrantShedsAnEarlierCombatScope() {
        CharacterSheet sheet = sheet();
        sheet.grantEnhancedAttacksForCombat(SOURCE, 3);
        sheet.grantEnhancedAttacks(SOURCE, 2);

        sheet.endCombat();

        assertEquals(2, sheet.getRemainingEnhancedAttacks(SOURCE));
    }

    @Test
    void combatCanStartAgainAfterItEnds() {
        Scene scene = new Scene();
        scene.addParticipant(sheet(), 10);
        scene.startCombat();
        scene.endCombat();

        scene.startCombat();

        assertTrue(scene.isCombatScene());
    }

    @Test
    void anActivationWindowLastsItsRodadasAndRenewsRatherThanStacks() {
        CharacterSheet sheet = sheet();

        sheet.openActivationWindow(SOURCE, 1);
        sheet.openActivationWindow(SOURCE, 1);
        assertTrue(sheet.hasActivationWindow(SOURCE));
        assertFalse(sheet.hasActivationWindow("another source"));

        sheet.finishTurn();
        assertFalse(sheet.hasActivationWindow(SOURCE));
    }
}
