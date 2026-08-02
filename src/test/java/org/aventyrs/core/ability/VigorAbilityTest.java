package org.aventyrs.core.ability;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.modifier.ModifierResolver;
import org.aventyrs.core.modifier.ModifierResolverImpl;
import org.aventyrs.core.modifier.ModifierType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class VigorAbilityTest {

    private final ModifierResolver modifierResolver = new ModifierResolverImpl();

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

    @Test
    void onlySobreHumanoGrantsALifeMultiplierBonus() {
        for (VigorAbility ability : VigorAbility.values()) {
            int expected = ability == VigorAbility.SOBRE_HUMANO ? 1 : 0;
            assertEquals(expected, modifierResolver.sumModifiers(ability, ModifierType.LIFE_MULTIPLIER));
        }
    }

    @Test
    void onlySangueDeGiganteGrantsASizeCategoryBonus() {
        for (VigorAbility ability : VigorAbility.values()) {
            int expected = ability == VigorAbility.SANGUE_DE_GIGANTE ? 1 : 0;
            assertEquals(expected, modifierResolver.sumModifiers(ability, ModifierType.SIZE_CATEGORY));
        }
    }
}
