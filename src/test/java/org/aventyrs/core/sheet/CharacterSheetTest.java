package org.aventyrs.core.sheet;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CharacterSheetTest {

    @BeforeEach
    public void setup() {
        CharacterFixture.loadTemplates();
    }

    private CharacterSheet newSheet() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();
        return CharacterSheet.of(character, new Player());
    }

    @Test
    void damageReducesAvailableHitPointsBudget() {
        CharacterSheet sheet = newSheet();
        assertEquals(5, sheet.applyDamage(5));
    }

    @Test
    void shieldAbsorbsDamageBeforeDamageTaken() {
        CharacterSheet sheet = newSheet();
        sheet.addShield(4);
        assertEquals(1, sheet.applyDamage(5));
        assertEquals(0, sheet.getShieldPoints());
    }

    @Test
    void curseDamageBypassesShield() {
        CharacterSheet sheet = newSheet();
        sheet.addShield(10);
        assertEquals(5, sheet.applyCurseDamage(5));
        assertEquals(10, sheet.getShieldPoints());
    }

    @Test
    void healReducesAccumulatedDamageNotBelowZero() {
        CharacterSheet sheet = newSheet();
        sheet.applyDamage(5);
        assertEquals(0, sheet.heal(10));
    }

    @Test
    void shieldPointsAccumulate() {
        CharacterSheet sheet = newSheet();
        sheet.addShield(3);
        assertEquals(5, sheet.addShield(2));
    }

    @Test
    void magicPointsSpentAreTrackedIndependentlyFromHitPoints() {
        CharacterSheet sheet = newSheet();
        sheet.applyDamage(5);
        assertEquals(3, sheet.spendMagicPoints(3));
        assertEquals(5, sheet.getDamageTaken());
    }

    @Test
    void recoverMagicPointsReducesSpentNotBelowZero() {
        CharacterSheet sheet = newSheet();
        sheet.spendMagicPoints(3);
        assertEquals(0, sheet.recoverMagicPoints(10));
    }

    @Test
    void determinationPointsSpentAreTrackedIndependentlyFromOtherPools() {
        CharacterSheet sheet = newSheet();
        sheet.applyDamage(5);
        sheet.spendMagicPoints(3);
        assertEquals(2, sheet.spendDeterminationPoints(2));
        assertEquals(5, sheet.getDamageTaken());
        assertEquals(3, sheet.getManaSpent());
    }

    @Test
    void recoverDeterminationPointsReducesSpentNotBelowZero() {
        CharacterSheet sheet = newSheet();
        sheet.spendDeterminationPoints(2);
        assertEquals(0, sheet.recoverDeterminationPoints(10));
    }

    @Test
    void temporaryEgoPointsStartAtZeroForEveryDomain() {
        CharacterSheet sheet = newSheet();
        for (EgoDomain domain : EgoDomain.values()) {
            assertEquals(0, sheet.getTemporaryEgoPoints(domain));
        }
    }

    @Test
    void gainTemporaryEgoPointsAccumulates() {
        CharacterSheet sheet = newSheet();
        sheet.gainTemporaryEgoPoints(EgoDomain.SORTE, 1);
        assertEquals(3, sheet.gainTemporaryEgoPoints(EgoDomain.SORTE, 2));
    }

    @Test
    void spendTemporaryEgoPointsReducesOnlyThatDomain() {
        CharacterSheet sheet = newSheet();
        sheet.gainTemporaryEgoPoints(EgoDomain.AUTOCONTROLE, 3);
        sheet.gainTemporaryEgoPoints(EgoDomain.SORTE, 3);

        assertEquals(1, sheet.spendTemporaryEgoPoints(EgoDomain.AUTOCONTROLE, 2));
        assertEquals(3, sheet.getTemporaryEgoPoints(EgoDomain.SORTE));
    }

    @Test
    void spendTemporaryEgoPointsNeverGoesBelowZero() {
        CharacterSheet sheet = newSheet();
        sheet.gainTemporaryEgoPoints(EgoDomain.RECURSOS, 1);
        assertEquals(0, sheet.spendTemporaryEgoPoints(EgoDomain.RECURSOS, 10));
    }
}
