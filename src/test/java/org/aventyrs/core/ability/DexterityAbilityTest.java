package org.aventyrs.core.ability;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.modifier.ModifierResolver;
import org.aventyrs.core.modifier.ModifierResolverImpl;
import org.aventyrs.core.modifier.ModifierType;
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

    @Test
    void onlyPassosLongosGrantsAMovementModifier() {
        ModifierResolver modifierResolver = new ModifierResolverImpl();
        for (DexterityAbility ability : DexterityAbility.values()) {
            int expected = ability == DexterityAbility.PASSOS_LONGOS ? 1 : 0;
            assertEquals(expected, modifierResolver.sumModifiers(ability, ModifierType.MOVEMENT));
        }
    }
}
