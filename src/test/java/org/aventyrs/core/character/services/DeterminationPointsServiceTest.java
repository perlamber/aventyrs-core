package org.aventyrs.core.character.services;

import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.Human;
import org.aventyrs.core.sheet.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DeterminationPointsServiceTest {

    private final DeterminationPointsService determinationPointsService = new DeterminationPointsServiceImpl();

    private Character characterWithInstinct(int instinctBase) {
        return Character.builder()
                .player(new Player())
                .name("Test")
                .race(new Human())
                .attributes(CharacterAttributes.builder()
                        .instinct(AttributeValue.builder().base(instinctBase).build())
                        .build())
                .build();
    }

    @Test
    void defaultDeterminationMultiplierIsThree() {
        Character character = characterWithInstinct(3);
        assertEquals(3, determinationPointsService.getDeterminationMultiplier(character));
    }

    @Test
    void maxDeterminationPointsIsInstinctTimesDeterminationMultiplier() {
        Character character = characterWithInstinct(3);
        assertEquals(9, determinationPointsService.getMaxDeterminationPoints(character));
    }
}
