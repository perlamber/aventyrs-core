package org.aventyrs.core.ability;

import org.aventyrs.core.character.AttributeDomain;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class GnoseAbilityTest {

    @Test
    void everyAbilityBelongsToGnose() {
        for (GnoseAbility ability : GnoseAbility.values()) {
            assertEquals(AttributeDomain.GNOSE, ability.getAttributeDomain());
        }
    }

    @Test
    void everyAbilityHasADescription() {
        for (GnoseAbility ability : GnoseAbility.values()) {
            assertFalse(ability.getDescription().isBlank());
        }
    }

    @Test
    void listHasTheFiveDescribedAbilities() {
        assertEquals(5, GnoseAbility.values().length);
    }
}
