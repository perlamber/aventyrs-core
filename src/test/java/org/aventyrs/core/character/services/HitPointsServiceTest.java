package org.aventyrs.core.character.services;

import org.aventyrs.core.ability.VigorAbility;
import org.aventyrs.core.action.ActionProfile;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.AttributeValue;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterAttributes;
import org.aventyrs.core.character.CharacterEgos;
import org.aventyrs.core.character.CharacterStatus;
import org.aventyrs.core.race.Human;
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
                .egos(CharacterEgos.builder().build())
                .attributes(CharacterAttributes.builder()
                        .vigor(AttributeValue.builder().domain(AttributeDomain.VIGOR).base(vigorBase).build())
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
    void maxHitPointsIsBasePointsPlusVigorTimesLifeMultiplier() {
        Character character = characterWithVigor(3);
        assertEquals(22, hitPointsService.getMaxHitPoints(character));
    }

    @Test
    void sobreHumanoIncreasesLifeMultiplierByOne() {
        Character character = characterWithVigor(3, VigorAbility.SOBRE_HUMANO);
        assertEquals(5, hitPointsService.getLifeMultiplier(character));
        assertEquals(25, hitPointsService.getMaxHitPoints(character));
    }

    @Test
    void currentHitPointsStartsAtMax() {
        Character character = characterWithVigor(3);
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        assertEquals(22, hitPointsService.getCurrentHitPoints(character, sheet));
    }

    @Test
    void currentHitPointsReflectsDamageTaken() {
        Character character = characterWithVigor(3);
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        sheet.applyDamage(5);
        assertEquals(17, hitPointsService.getCurrentHitPoints(character, sheet));
    }

    @Test
    void currentHitPointsNeverGoesBelowZero() {
        Character character = characterWithVigor(3);
        CharacterSheet sheet = CharacterSheet.of(character, new Player());
        sheet.applyDamage(1000);
        assertEquals(0, hitPointsService.getCurrentHitPoints(character, sheet));
    }

    @Test
    void statusIsCleanAtFullHitPoints() {
        assertEquals(CharacterStatus.CLEAN, hitPointsService.getStatus(20, 20));
    }

    @Test
    void statusIsHighLifeAboveTwoThirds() {
        assertEquals(CharacterStatus.HIGH_LIFE, hitPointsService.getStatus(19, 20));
        assertEquals(CharacterStatus.HIGH_LIFE, hitPointsService.getStatus(14, 20));
    }

    @Test
    void statusIsMediumLifeAboveOneThird() {
        assertEquals(CharacterStatus.MEDIUM_LIFE, hitPointsService.getStatus(13, 20));
        assertEquals(CharacterStatus.MEDIUM_LIFE, hitPointsService.getStatus(7, 20));
    }

    @Test
    void statusIsLowLifeAboveZero() {
        assertEquals(CharacterStatus.LOW_LIFE, hitPointsService.getStatus(6, 20));
        assertEquals(CharacterStatus.LOW_LIFE, hitPointsService.getStatus(1, 20));
    }

    @Test
    void statusIsFallenFromZeroToAboveNegativeHalfOfMax() {
        assertEquals(CharacterStatus.FALLEN, hitPointsService.getStatus(0, 20));
        assertEquals(CharacterStatus.FALLEN, hitPointsService.getStatus(-9, 20));
    }

    @Test
    void statusIsCommaFromNegativeHalfOfMaxToAboveNegativeMax() {
        assertEquals(CharacterStatus.COMMA, hitPointsService.getStatus(-10, 20));
        assertEquals(CharacterStatus.COMMA, hitPointsService.getStatus(-19, 20));
    }

    @Test
    void statusIsDeadAtOrBelowNegativeMax() {
        assertEquals(CharacterStatus.DEAD, hitPointsService.getStatus(-20, 20));
        assertEquals(CharacterStatus.DEAD, hitPointsService.getStatus(-1000, 20));
    }
}
