package org.aventyrs.core.character.services;

import org.aventyrs.core.ability.VigorAbility;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.TitleSlot;
import org.aventyrs.core.character.fixture.CharacterFixture;
import org.aventyrs.core.feat.VampiricoFeat;
import org.aventyrs.core.race.Human;
import org.aventyrs.core.race.Vampiro;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.LifeSteal;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.title.santo.Santo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
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

    private Character vampiroWithSedeDeSangue(final int titles) {
        Character character = CharacterFixture.blank(CharacterFixture.BLANK)
                .race(new Vampiro(Vampiro.VampiroLineage.NOSFERATU, new Human()))
                .feats(new ArrayList<>(List.of(VampiricoFeat.SEDE_DE_SANGUE)))
                .build();
        TitleSlot[] slots = {TitleSlot.PRIMARY, TitleSlot.SECONDARY, TitleSlot.TERTIARY};
        for (int i = 0; i < titles; i++) {
            character.grantTitle(new Santo(List.of(), List.of()), slots[i]);
        }
        return character;
    }

    @Test
    void sedeDeSangueAmplifiesAnActiveLifeStealByTitulosDespertos() {
        Character character = vampiroWithSedeDeSangue(2);
        CharacterSheet sheet = sheet(character);
        sheet.applyEffect(new LifeSteal(1, Optional.of(2)));

        assertEquals(1 + 2, lifeStealService.getTotalLifeSteal(character, sheet));
    }

    @Test
    void sedeDeSangueGrantsNothingWithNoActiveLifeSteal() {
        Character character = vampiroWithSedeDeSangue(2);
        assertEquals(0, lifeStealService.getTotalLifeSteal(character, sheet(character)));
    }
}
