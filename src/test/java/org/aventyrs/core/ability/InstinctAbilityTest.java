package org.aventyrs.core.ability;

import org.aventyrs.core.action.ActionProfile;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterEgos;
import org.aventyrs.core.modifier.ModifierResolver;
import org.aventyrs.core.modifier.ModifierResolverImpl;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.race.Human;
import org.aventyrs.core.rest.RestType;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.title.santo.Santo;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class InstinctAbilityTest {

    private final ModifierResolver modifierResolver = new ModifierResolverImpl();

    private Character.CharacterBuilder baseCharacter() {
        return Character.builder()
                .player(new Player())
                .name("Test")
                .race(new Human())
                .actionProfile(ActionProfile.REFLEXOS_RAPIDOS)
                .egos(CharacterEgos.builder().build())
                .attributes(CharacterAttributes.builder().build());
    }

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

    @Test
    void onlySupermotivadoGrantsARestDeterminationPointsBonus() {
        for (InstinctAbility ability : InstinctAbility.values()) {
            for (RestType restType : RestType.values()) {
                boolean grantsBonus = ability == InstinctAbility.SUPERMOTIVADO;
                int expected = grantsBonus ? (restType.isAtLeast(RestType.LONGO) ? 3 : 1) : 0;
                assertEquals(expected, ability.resolveRestDeterminationPointsBonus(restType));
            }
        }
    }

    @Test
    void onlySentirAIntencaoGrantsAnAttributeDomainRollBonusWhenNoTitleIsHeld() {
        Character titleless = baseCharacter().build();
        for (InstinctAbility ability : InstinctAbility.values()) {
            int expected = ability == InstinctAbility.SENTIR_A_INTENCAO ? Skill.ADVANTAGE_BONUS : 0;
            assertEquals(expected, ability.resolveAttributeDomainRollBonus(AttributeDomain.INSTINCT, titleless));
        }
    }

    @Test
    void sentirAIntencaoGrantsNoRollBonusOnceATitleIsHeld() {
        Character titled = baseCharacter().primaryTitle(new Santo(List.of(), List.of())).build();
        assertEquals(0, InstinctAbility.SENTIR_A_INTENCAO.resolveAttributeDomainRollBonus(AttributeDomain.INSTINCT, titled));
    }

    @Test
    void sentirAIntencaoGrantsADifficultyReductionOnceATitleIsHeld() {
        Character titleless = baseCharacter().build();
        Character titled = baseCharacter().primaryTitle(new Santo(List.of(), List.of())).build();

        assertEquals(0, InstinctAbility.SENTIR_A_INTENCAO.resolveAttributeDomainDifficultyReduction(AttributeDomain.INSTINCT, titleless));
        assertEquals(1, InstinctAbility.SENTIR_A_INTENCAO.resolveAttributeDomainDifficultyReduction(AttributeDomain.INSTINCT, titled));
    }

    @Test
    void sentirAIntencaoGrantsNothingForANonInstinctDomain() {
        Character titleless = baseCharacter().build();
        Character titled = baseCharacter().primaryTitle(new Santo(List.of(), List.of())).build();

        assertEquals(0, InstinctAbility.SENTIR_A_INTENCAO.resolveAttributeDomainRollBonus(AttributeDomain.CHARISMA, titleless));
        assertEquals(0, InstinctAbility.SENTIR_A_INTENCAO.resolveAttributeDomainDifficultyReduction(AttributeDomain.CHARISMA, titled));
    }
}
