package org.aventyrs.core.ability;

import org.aventyrs.core.character.AttributeDomain;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class StrengthAbilityTest {

    @Test
    void everyAbilityBelongsToStrength() {
        for (StrengthAbility ability : StrengthAbility.values()) {
            assertEquals(AttributeDomain.STRENGTH, ability.getAttributeDomain());
        }
    }

    @Test
    void everyAbilityHasADescription() {
        for (StrengthAbility ability : StrengthAbility.values()) {
            assertFalse(ability.getDescription().isBlank());
        }
    }

    @Test
    void listHasTheFiveDescribedAbilities() {
        assertEquals(5, StrengthAbility.values().length);
    }
}
