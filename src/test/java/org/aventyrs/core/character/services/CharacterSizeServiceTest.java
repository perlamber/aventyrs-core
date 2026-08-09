package org.aventyrs.core.character.services;

import org.aventyrs.core.ability.VigorAbility;
import org.aventyrs.core.action.ActionProfile;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterEgos;
import org.aventyrs.core.character.SizeCategory;
import org.aventyrs.core.race.Human;
import org.aventyrs.core.sheet.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CharacterSizeServiceTest {

    private final CharacterSizeService sizeService = new CharacterSizeServiceImpl();

    private Character characterOfSize(SizeCategory sizeCategory, VigorAbility... abilities) {
        Character.CharacterBuilder builder = Character.builder()
                .player(new Player())
                .name("Test")
                .race(new Human())
                .actionProfile(ActionProfile.REFLEXOS_RAPIDOS)
                .egos(CharacterEgos.builder().build())
                .attributes(CharacterAttributes.builder().build())
                .sizeCategory(sizeCategory);
        for (VigorAbility ability : abilities) {
            builder.attributeAbility(ability);
        }
        return builder.build();
    }

    @Test
    void defaultsToTheCharactersOwnSizeCategory() {
        Character character = characterOfSize(SizeCategory.ZERO);
        assertEquals(SizeCategory.ZERO, sizeService.getEffectiveSizeCategory(character));
    }

    @Test
    void sangueDeGiganteIncreasesEffectiveSizeCategoryByOne() {
        Character character = characterOfSize(SizeCategory.ZERO, VigorAbility.SANGUE_DE_GIGANTE);
        assertEquals(SizeCategory.PLUS_ONE, sizeService.getEffectiveSizeCategory(character));
    }

    @Test
    void bonusIsClampedAtThePlusFourCeiling() {
        Character character = characterOfSize(SizeCategory.PLUS_FOUR, VigorAbility.SANGUE_DE_GIGANTE);
        assertEquals(SizeCategory.PLUS_FOUR, sizeService.getEffectiveSizeCategory(character));
    }
}
