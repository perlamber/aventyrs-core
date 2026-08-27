package org.aventyrs.core.ability;

import org.aventyrs.core.action.ActionProfile;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterEgos;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.character.SizeCategory;
import org.aventyrs.core.modifier.ModifierResolver;
import org.aventyrs.core.modifier.ModifierResolverImpl;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.race.Human;
import org.aventyrs.core.rest.RestType;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class VigorAbilityTest {

    private final ModifierResolver modifierResolver = new ModifierResolverImpl();

    private CharacterSheet characterSheetOfSize(final SizeCategory sizeCategory) {
        Character character = Character.builder()
                .player(new Player())
                .name("Test")
                .race(new Human())
                .actionProfile(ActionProfile.REFLEXOS_RAPIDOS)
                .egos(CharacterEgos.builder().build())
                .attributes(CharacterAttributes.builder().build())
                .sizeCategory(sizeCategory)
                .build();
        return CharacterSheet.of(character, new Player());
    }

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

    @Test
    void onlyMetabolismoRapidoGrantsARestHitPointsBonus() {
        for (VigorAbility ability : VigorAbility.values()) {
            for (RestType restType : RestType.values()) {
                boolean grantsBonus = ability == VigorAbility.METABOLISMO_RAPIDO && restType.isAtLeast(RestType.LONGO);
                int expected = grantsBonus ? 3 : 0;
                assertEquals(expected, ability.resolveRestHitPointsBonus(restType));
            }
        }
    }

    @Test
    void onlyMetabolismoRapidoGrantsALifeStealBonus() {
        for (VigorAbility ability : VigorAbility.values()) {
            int expected = ability == VigorAbility.METABOLISMO_RAPIDO ? 1 : 0;
            assertEquals(expected, ability.resolveLifeStealBonus());
        }
    }

    @Test
    void onlyRigidezDaMontanhaGrantsADamageReductionAgainstFisicoDamageWithNoKnownSource() {
        CharacterSheet target = characterSheetOfSize(SizeCategory.ZERO);
        for (VigorAbility ability : VigorAbility.values()) {
            int expected = ability == VigorAbility.RIGIDEZ_DA_MONTANHA ? 1 : 0;
            assertEquals(expected, ability.resolveDamageReduction(DamageType.FISICO, null, target));
        }
    }

    @Test
    void rigidezDaMontanhaGrantsNothingAgainstNonFisicoDamage() {
        CharacterSheet target = characterSheetOfSize(SizeCategory.ZERO);
        for (DamageType damageType : DamageType.values()) {
            if (damageType != DamageType.FISICO) {
                assertEquals(0, VigorAbility.RIGIDEZ_DA_MONTANHA.resolveDamageReduction(damageType, null, target));
            }
        }
    }

    @Test
    void rigidezDaMontanhaReducesByTwoWhenTheAttackerIsSmaller() {
        CharacterSheet target = characterSheetOfSize(SizeCategory.ZERO);
        CharacterSheet source = characterSheetOfSize(SizeCategory.MINUS_ONE);

        assertEquals(2, VigorAbility.RIGIDEZ_DA_MONTANHA.resolveDamageReduction(DamageType.FISICO, source, target));
    }

    @Test
    void rigidezDaMontanhaReducesByOneWhenTheAttackerIsNotSmaller() {
        CharacterSheet target = characterSheetOfSize(SizeCategory.ZERO);
        CharacterSheet sameSize = characterSheetOfSize(SizeCategory.ZERO);
        CharacterSheet larger = characterSheetOfSize(SizeCategory.PLUS_ONE);

        assertEquals(1, VigorAbility.RIGIDEZ_DA_MONTANHA.resolveDamageReduction(DamageType.FISICO, sameSize, target));
        assertEquals(1, VigorAbility.RIGIDEZ_DA_MONTANHA.resolveDamageReduction(DamageType.FISICO, larger, target));
    }
}
