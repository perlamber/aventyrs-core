package org.aventyrs.core.character.services;

import org.aventyrs.core.ability.FocusAbility;
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

class MagicPointsServiceTest {

    private final MagicPointsService magicPointsService = new MagicPointsServiceImpl();

    private Character characterWithFocus(int focusBase, FocusAbility... abilities) {
        return characterWithFocus(focusBase, MagicPointsService.DEFAULT_MANA_MULTIPLIER, abilities);
    }

    private Character characterWithFocus(int focusBase, int manaMultiplier, FocusAbility... abilities) {
        Character.CharacterBuilder builder = Character.builder()
                .player(new Player())
                .name("Test")
                .race(new Human())
                .actionProfile(ActionProfile.REFLEXOS_RAPIDOS)
                .egos(CharacterEgos.builder().build())
                .manaMultiplier(manaMultiplier)
                .attributes(CharacterAttributes.builder()
                        .focus(AttributeValue.builder().domain(AttributeDomain.FOCUS).base(focusBase).build())
                        .build());
        for (FocusAbility ability : abilities) {
            builder.attributeAbility(ability);
        }
        return builder.build();
    }

    @Test
    void defaultManaMultiplierIsThree() {
        Character character = characterWithFocus(3);
        assertEquals(3, magicPointsService.getManaMultiplier(character));
    }

    @Test
    void maxMagicPointsIsBasePointsPlusFocusTimesManaMultiplier() {
        Character character = characterWithFocus(3);
        assertEquals(19, magicPointsService.getMaxMagicPoints(character));
    }

    @Test
    void maxMagicPointsWithOneFocusIsThirteen() {
        Character character = characterWithFocus(1);
        assertEquals(13, magicPointsService.getMaxMagicPoints(character));
    }

    @Test
    void manaMultiplierIsEditablePerCharacter() {
        Character character = characterWithFocus(3, 5);
        assertEquals(5, magicPointsService.getManaMultiplier(character));
        assertEquals(25, magicPointsService.getMaxMagicPoints(character));
    }

    @Test
    void conexaoComOManaIncreasesManaMultiplierByOne() {
        Character character = characterWithFocus(3, MagicPointsService.DEFAULT_MANA_MULTIPLIER, FocusAbility.CONEXAO_COM_O_MANA);
        assertEquals(4, magicPointsService.getManaMultiplier(character));
        assertEquals(22, magicPointsService.getMaxMagicPoints(character));
    }

    @Test
    void currentMagicPointsReflectsSpending() {
        Character character = characterWithFocus(3);
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        sheet.spendMagicPoints(4);
        assertEquals(15, magicPointsService.getCurrentMagicPoints(character, sheet));
    }

    @Test
    void currentMagicPointsNeverGoesBelowZero() {
        Character character = characterWithFocus(3);
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        sheet.spendMagicPoints(1000);
        assertEquals(0, magicPointsService.getCurrentMagicPoints(character, sheet));
    }
}
