package org.aventyrs.core.sheet;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.Human;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CharacterSheetTest {

    private CharacterSheet newSheet() {
        Character character = Character.builder()
                .player(new Player())
                .name("Test")
                .race(new Human())
                .attributes(CharacterAttributes.builder().build())
                .build();
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
}
