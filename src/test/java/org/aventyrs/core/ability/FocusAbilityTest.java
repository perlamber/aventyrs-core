package org.aventyrs.core.ability;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.modifier.ModifierResolver;
import org.aventyrs.core.modifier.ModifierResolverImpl;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.rest.RestType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class FocusAbilityTest {

    private final ModifierResolver modifierResolver = new ModifierResolverImpl();

    @Test
    void everyAbilityBelongsToFocus() {
        for (FocusAbility ability : FocusAbility.values()) {
            assertEquals(AttributeDomain.FOCUS, ability.getAttributeDomain());
        }
    }

    @Test
    void everyAbilityHasADescription() {
        for (FocusAbility ability : FocusAbility.values()) {
            assertFalse(ability.getDescription().isBlank());
        }
    }

    @Test
    void listHasTheFiveDescribedAbilities() {
        assertEquals(5, FocusAbility.values().length);
    }

    @Test
    void onlyConexaoComOManaGrantsAManaMultiplierBonus() {
        for (FocusAbility ability : FocusAbility.values()) {
            int expected = ability == FocusAbility.CONEXAO_COM_O_MANA ? 1 : 0;
            assertEquals(expected, modifierResolver.sumModifiers(ability, ModifierType.MANA_MULTIPLIER));
        }
    }

    @Test
    void onlyConcentracaoProfundaResolvesAnActiveAbility() {
        for (FocusAbility ability : FocusAbility.values()) {
            boolean expected = ability == FocusAbility.CONCENTRACAO_PROFUNDA;
            assertEquals(expected, ability.resolveActiveAbility().isPresent());
        }
    }

    @Test
    void onlyCanalizadorDeManaGrantsARestMagicPointsBonus() {
        for (FocusAbility ability : FocusAbility.values()) {
            for (RestType restType : RestType.values()) {
                boolean grantsBonus = ability == FocusAbility.CANALIZADOR_DE_MANA && restType.isAtLeast(RestType.LONGO);
                int expected = grantsBonus ? 2 : 0;
                assertEquals(expected, ability.resolveRestMagicPointsBonus(restType));
            }
        }
    }
}
