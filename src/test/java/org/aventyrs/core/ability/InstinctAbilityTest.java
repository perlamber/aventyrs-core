package org.aventyrs.core.ability;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.modifier.ModifierResolver;
import org.aventyrs.core.modifier.ModifierResolverImpl;
import org.aventyrs.core.modifier.ModifierType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class InstinctAbilityTest {

    private final ModifierResolver modifierResolver = new ModifierResolverImpl();

    @Test
    void everyAbilityBelongsToInstinct() {
        for (InstinctAbility ability : InstinctAbility.values()) {
            assertEquals(AttributeDomain.INSTINCT, ability.getAttributeDomain());
        }
    }

    @Test
    void everyAbilityHasADescription() {
        for (InstinctAbility ability : InstinctAbility.values()) {
            assertFalse(ability.getDescription().isBlank());
        }
    }

    @Test
    void listHasTheFiveDescribedAbilities() {
        assertEquals(5, InstinctAbility.values().length);
    }

    @Test
    void onlyObstinadoGrantsADeterminationMultiplierBonus() {
        for (InstinctAbility ability : InstinctAbility.values()) {
            int expected = ability == InstinctAbility.OBSTINADO ? 1 : 0;
            assertEquals(expected, modifierResolver.sumModifiers(ability, ModifierType.DETERMINATION_MULTIPLIER));
        }
    }
}
