package org.aventyrs.core.character.services;

import org.aventyrs.core.ability.VigorAbility;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.LifeSteal;
import org.aventyrs.core.sheet.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LifeStealServiceImplTest {

    private final LifeStealService lifeStealService = new LifeStealServiceImpl();

    @BeforeEach
    void setup() {
        CharacterFixture.loadTemplates();
    }

    private CharacterSheet sheet(final Character character) {
        return CharacterSheet.of(character, new Player());
    }

    @Test
    void getTotalLifeStealIsZeroWithNoActiveEffect() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();
        CharacterSheet sheet = sheet(character);

        assertEquals(0, lifeStealService.getTotalLifeSteal(character, sheet));
    }

    @Test
    void getTotalLifeStealMatchesTheSheetsOwnValueWithoutMetabolismoRapido() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK).build();
        CharacterSheet sheet = sheet(character);
        sheet.applyEffect(new LifeSteal(2, Optional.of(3)));

        assertEquals(2, lifeStealService.getTotalLifeSteal(character, sheet));
    }

    @Test
    void metabolismoRapidoAddsItsFlatBonusOnceAnEffectIsAlreadyActive() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributeAbility(VigorAbility.METABOLISMO_RAPIDO)
                .build();
        CharacterSheet sheet = sheet(character);
        sheet.applyEffect(new LifeSteal(2, Optional.of(3)));

        assertEquals(3, lifeStealService.getTotalLifeSteal(character, sheet));
    }

    @Test
    void metabolismoRapidoGrantsNothingWithNoActiveEffectOfItsOwn() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributeAbility(VigorAbility.METABOLISMO_RAPIDO)
                .build();
        CharacterSheet sheet = sheet(character);

        assertEquals(0, lifeStealService.getTotalLifeSteal(character, sheet));
    }

    @Test
    void metabolismoRapidosBonusDoesNotStackAcrossMultipleActiveEffects() {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .attributeAbility(VigorAbility.METABOLISMO_RAPIDO)
                .build();
        CharacterSheet sheet = sheet(character);
        sheet.applyEffect(new LifeSteal(2, Optional.of(3)));
        sheet.applyEffect(new LifeSteal(3, Optional.of(3)));

        // 2 + 3 (both effects) + 1 (METABOLISMO_RAPIDO's own flat, non-stacking bonus).
        assertEquals(6, lifeStealService.getTotalLifeSteal(character, sheet));
    }
}
