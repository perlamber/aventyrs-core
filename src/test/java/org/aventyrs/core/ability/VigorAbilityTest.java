package org.aventyrs.core.ability;

import org.aventyrs.core.character.AttributeDomain;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class VigorAbilityTest {

    @Test
    void everyAbilityBelongsToVigor() {
        for (VigorAbility ability : VigorAbility.values()) {
            assertEquals(AttributeDomain.VIGOR, ability.getAttributeDomain());
        }
    }

    @Test
    void everyAbilityHasADescription() {
        for (VigorAbility ability : VigorAbility.values()) {
            assertFalse(ability.getDescription().isBlank());
        }
    }

    @Test
    void listHasTheFiveDescribedAbilities() {
        assertEquals(5, VigorAbility.values().length);
    }
}
