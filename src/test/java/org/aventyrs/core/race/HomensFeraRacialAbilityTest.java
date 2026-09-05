package org.aventyrs.core.race;

import org.aventyrs.core.action.ActionProfile;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterEgos;
import org.aventyrs.core.character.services.MovementService;
import org.aventyrs.core.character.services.MovementServiceImpl;
import org.aventyrs.core.modifier.ModifierResolver;
import org.aventyrs.core.modifier.ModifierResolverImpl;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.sheet.Player;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class HomensFeraRacialAbilityTest {

    private final MovementService movementService = new MovementServiceImpl();

    private Character characterOf(final Race race) {
        return race.generateEmptyCharacter(List.of())
                .player(new Player())
                .name("Test")
                .race(race)
                .attributes(CharacterAttributes.builder().build())
                .egos(CharacterEgos.builder().build())
                .actionProfile(ActionProfile.REFLEXOS_RAPIDOS)
                .build();
    }

    @Test
    void everyAbilityHasADescription() {
        for (HomensFeraRacialAbility ability : HomensFeraRacialAbility.values()) {
            assertFalse(ability.getDescription().isBlank());
        }
    }

    @Test
    void fortalecimentoFeralGrantsAFlatMovementModifier() {
        ModifierResolver modifierResolver = new ModifierResolverImpl();

        assertEquals(1, modifierResolver.sumModifiers(
                HomensFeraRacialAbility.FORTALECIMENTO_FERAL, ModifierType.MOVEMENT));
    }

    @Test
    void fortalecimentoFeralRaisesMovimentoBaseByOneUnidadeDeDistancia() {
        int baseline = movementService.getMovementBase(characterOf(new Human()));

        assertEquals(baseline + 1,
                movementService.getMovementBase(characterOf(new HomemFera(HomemFera.EspiritoAnimal.LICANTROPO))));
    }

    @Test
    void everySpiritGrantsTheSameMovimentoBase() {
        int expected = movementService.getMovementBase(characterOf(new Human())) + 1;
        for (HomemFera.EspiritoAnimal espirito : HomemFera.EspiritoAnimal.values()) {
            assertEquals(expected, movementService.getMovementBase(characterOf(new HomemFera(espirito))));
        }
    }
}
