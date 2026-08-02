package org.aventyrs.core.ability;

import org.aventyrs.core.character.AttributeDomain;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class DexterityAbilityTest {

    @Test
    void everyAbilityBelongsToDexterity() {
        for (DexterityAbility ability : DexterityAbility.values()) {
            assertEquals(AttributeDomain.DEXTERITY, ability.getAttributeDomain());
        }
    }

    @Test
    void everyAbilityHasADescription() {
        for (DexterityAbility ability : DexterityAbility.values()) {
            assertFalse(ability.getDescription().isBlank());
        }
    }

    @Test
    void listHasTheFiveDescribedAbilities() {
        assertEquals(5, DexterityAbility.values().length);
    }
}
