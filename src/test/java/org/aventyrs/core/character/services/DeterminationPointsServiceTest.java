package org.aventyrs.core.character.services;

import org.aventyrs.core.ability.InstinctAbility;
import org.aventyrs.core.action.ActionProfile;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterEgos;
import org.aventyrs.core.race.Human;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DeterminationPointsServiceTest {

    private final DeterminationPointsService determinationPointsService = new DeterminationPointsServiceImpl();

    private Character characterWithInstinct(int instinctBase, InstinctAbility... abilities) {
        Character.CharacterBuilder builder = Character.builder()
                .player(new Player())
                .name("Test")
                .race(new Human())
                .actionProfile(ActionProfile.REFLEXOS_RAPIDOS)
                .egos(CharacterEgos.builder().build())
                .attributes(CharacterAttributes.builder()
                        .instinct(AttributeValue.builder().domain(AttributeDomain.INSTINCT).base(instinctBase).build())
                        .build());
        for (InstinctAbility ability : abilities) {
            builder.attributeAbility(ability);
        }
        return builder.build();
    }

    @Test
    void defaultDeterminationMultiplierIsThree() {
        Character character = characterWithInstinct(3);
        assertEquals(3, determinationPointsService.getDeterminationMultiplier(character));
    }

    @Test
    void maxDeterminationPointsIsBasePointsPlusInstinctTimesDeterminationMultiplier() {
        Character character = characterWithInstinct(3);
        assertEquals(19, determinationPointsService.getMaxDeterminationPoints(character));
    }

    @Test
    void obstinadoIncreasesDeterminationMultiplierByOne() {
        Character character = characterWithInstinct(3, InstinctAbility.OBSTINADO);
        assertEquals(4, determinationPointsService.getDeterminationMultiplier(character));
        assertEquals(22, determinationPointsService.getMaxDeterminationPoints(character));
    }

    @Test
    void currentDeterminationPointsReflectsSpending() {
        Character character = characterWithInstinct(3);
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        sheet.spendDeterminationPoints(4);
        assertEquals(15, determinationPointsService.getCurrentDeterminationPoints(character, sheet));
    }

    @Test
    void currentDeterminationPointsNeverGoesBelowZero() {
        Character character = characterWithInstinct(3);
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        sheet.spendDeterminationPoints(1000);
        assertEquals(0, determinationPointsService.getCurrentDeterminationPoints(character, sheet));
    }
}
