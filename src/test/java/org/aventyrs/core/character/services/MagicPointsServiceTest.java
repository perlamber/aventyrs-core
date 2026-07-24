package org.aventyrs.core.character.services;

import org.aventyrs.core.ability.FocusAbility;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.Human;
import org.aventyrs.core.sheet.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MagicPointsServiceTest {

    private final MagicPointsService magicPointsService = new MagicPointsServiceImpl();

    private Character characterWithFocus(int focusBase, FocusAbility... abilities) {
        Character.CharacterBuilder builder = Character.builder()
                .player(new Player())
                .name("Test")
                .race(new Human())
                .attributes(CharacterAttributes.builder()
                        .focus(AttributeValue.builder().base(focusBase).build())
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
    void maxMagicPointsIsFocusTimesManaMultiplier() {
        Character character = characterWithFocus(3);
        assertEquals(9, magicPointsService.getMaxMagicPoints(character));
    }

    @Test
    void conexaoComOManaIncreasesManaMultiplierByOne() {
        Character character = characterWithFocus(3, FocusAbility.CONEXAO_COM_O_MANA);
        assertEquals(4, magicPointsService.getManaMultiplier(character));
        assertEquals(12, magicPointsService.getMaxMagicPoints(character));
    }
}
