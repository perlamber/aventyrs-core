package org.aventyrs.core.ability;

import org.aventyrs.core.character.AttributeDomain;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CharismaAbilityTest {

    @Test
    void everyAbilityBelongsToCharisma() {
        for (CharismaAbility ability : CharismaAbility.values()) {
            assertEquals(AttributeDomain.CHARISMA, ability.getAttributeDomain());
        }
    }

    @Test
    void everyAbilityHasADescription() {
        for (CharismaAbility ability : CharismaAbility.values()) {
            assertFalse(ability.getDescription().isBlank());
        }
    }

    @Test
    void listHasTheFiveDescribedAbilities() {
        assertEquals(5, CharismaAbility.values().length);
    }
}
