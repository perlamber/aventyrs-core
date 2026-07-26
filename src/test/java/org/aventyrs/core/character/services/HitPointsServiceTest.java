package org.aventyrs.core.character.services;

import org.aventyrs.core.ability.VigorAbility;
import org.aventyrs.core.action.ActionProfile;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.Human;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HitPointsServiceTest {

    private final HitPointsService hitPointsService = new HitPointsServiceImpl();

    private Character characterWithVigor(int vigorBase, VigorAbility... abilities) {
        Character.CharacterBuilder builder = Character.builder()
                .player(new Player())
                .name("Test")
                .race(new Human())
                .actionProfile(ActionProfile.REFLEXOS_RAPIDOS)
                .attributes(CharacterAttributes.builder()
                        .vigor(AttributeValue.builder().base(vigorBase).build())
                        .build());
        for (VigorAbility ability : abilities) {
            builder.attributeAbility(ability);
        }
        return builder.build();
    }

    @Test
    void defaultLifeMultiplierIsFour() {
        Character character = characterWithVigor(3);
        assertEquals(4, hitPointsService.getLifeMultiplier(character));
    }

    @Test
    void maxHitPointsIsVigorTimesLifeMultiplier() {
        Character character = characterWithVigor(3);
        assertEquals(12, hitPointsService.getMaxHitPoints(character));
    }

    @Test
    void sobreHumanoIncreasesLifeMultiplierByOne() {
        Character character = characterWithVigor(3, VigorAbility.SOBRE_HUMANO);
        assertEquals(5, hitPointsService.getLifeMultiplier(character));
        assertEquals(15, hitPointsService.getMaxHitPoints(character));
    }

    @Test
    void currentHitPointsStartsAtMax() {
        Character character = characterWithVigor(3);
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        assertEquals(12, hitPointsService.getCurrentHitPoints(character, sheet));
    }

    @Test
    void currentHitPointsReflectsDamageTaken() {
        Character character = characterWithVigor(3);
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        sheet.applyDamage(5);
        assertEquals(7, hitPointsService.getCurrentHitPoints(character, sheet));
    }

    @Test
    void currentHitPointsNeverGoesBelowZero() {
        Character character = characterWithVigor(3);
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        sheet.applyDamage(1000);
        assertEquals(0, hitPointsService.getCurrentHitPoints(character, sheet));
    }
}
